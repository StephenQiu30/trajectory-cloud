package com.trajectory.cloud.gateway.filter;

import com.trajectory.cloud.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private static final Set<String> SKIP_LOG_PATHS = Set.of("/actuator");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }
        String path = request.getPath().value();
        if (SKIP_LOG_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String clientIp = resolveClientIp(request);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(GatewayConstant.HEADER_TRACE_ID, traceId)
                .build();

        log.info("[Log] ==> {} {} from {} [TraceID: {}]", method, path, clientIp, traceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build()).then(
                Mono.fromRunnable(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 0;
                    log.info("[Log] <== {} {} [{}] {}ms [TraceID: {}]", method, path, statusCode, latencyMs, traceId);
                }));
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
