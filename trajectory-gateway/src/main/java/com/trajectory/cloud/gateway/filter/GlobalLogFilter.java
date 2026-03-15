package com.trajectory.cloud.gateway.filter;

import com.trajectory.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.trajectory.cloud.common.constants.SecurityConstant;
import com.trajectory.cloud.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志全局过滤器
 * <p>
 * 记录每个请求的起止时间、状态码、客户端IP 等信息，
 * 并在响应完成后异步上报到日志服务（trajectory-log-service）。
 * </p>
 * <p>
 * 执行顺序: order = -200（在 HeaderSanitize 之后、Auth 之前），
 * 用于在请求最早阶段记录起始时间并注入 traceId。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private final WebClient logWebClient;

    /**
     * 不需要记录日志的路径前缀
     */
    private static final Set<String> SKIP_LOG_PATHS = Set.of(
            "/actuator"
    );

    /**
     * 日志上报失败限频：每 60 秒最多输出一次错误日志
     */
    private final AtomicLong lastErrorLogTime = new AtomicLong(0);
    private static final long ERROR_LOG_INTERVAL_MS = 60_000;

    public GlobalLogFilter(WebClient.Builder webClientBuilder) {
        this.logWebClient = webClientBuilder
                .baseUrl(GatewayConstant.LOG_SERVICE_BASE_URL)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 跳过 OPTIONS 预检请求（CORS），不记录日志
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // 跳过不需要记录日志的路径
        String path = request.getPath().value();
        if (shouldSkipLog(path)) {
            return chain.filter(exchange);
        }

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 生成链路追踪ID
        String traceId = UUID.randomUUID().toString();

        // 提取请求基本信息
        final String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        final String query = request.getURI().getQuery();
        final String clientIp = resolveClientIp(request);
        final String userAgent = request.getHeaders().getFirst("User-Agent");
        final String referer = request.getHeaders().getFirst("Referer");

        // 将 traceId 注入下游请求头，实现链路追踪
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(GatewayConstant.HEADER_TRACE_ID, traceId)
                .build();

        log.info("[Log] ==> {} {} from {} [TraceID: {}]", method, path, clientIp, traceId);

        // 执行后续过滤器链，并在响应完成后记录日志
        return chain.filter(exchange.mutate().request(mutatedRequest).build()).then(
                Mono.fromRunnable(() -> {
                    long latencyMs = System.currentTimeMillis() - startTime;

                    int statusCode = 0;
                    if (exchange.getResponse().getStatusCode() != null) {
                        statusCode = exchange.getResponse().getStatusCode().value();
                    }

                    // 从 Exchange 属性中获取登录用户ID（由 GlobalAuthFilter 写入）
                    Long loginUserId = resolveLoginUserId(exchange);

                    log.info("[Log] <== {} {} [{}] {}ms [TraceID: {}]",
                            method, path, statusCode, latencyMs, traceId);

                    // 异步上报到日志服务
                    submitAccessLog(traceId, path, method, query,
                            statusCode, (int) latencyMs, clientIp, userAgent, referer, loginUserId);
                }));
    }

    /**
     * 判断是否跳过日志记录
     */
    private boolean shouldSkipLog(String path) {
        return SKIP_LOG_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 解析客户端真实IP
     * <p>
     * 优先从 X-Forwarded-For / X-Real-IP 请求头获取（经过反向代理时），
     * 否则取 remoteAddress
     * </p>
     */
    private String resolveClientIp(ServerHttpRequest request) {
        // 尝试从代理头获取真实IP
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 从 remoteAddress 获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    /**
     * 从 Exchange 属性中解析登录用户ID
     */
    private Long resolveLoginUserId(ServerWebExchange exchange) {
        Object attr = exchange.getAttribute(GatewayConstant.ATTR_LOGIN_USER_ID);
        if (attr == null) {
            return null;
        }
        try {
            return Long.parseLong(attr.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 异步上报 API 访问日志到日志服务（fire-and-forget，不阻塞主链路）
     * <p>
     * 上报失败时进行限频日志输出，避免日志服务不可用时产生日志爆炸。
     * </p>
     */
    private void submitAccessLog(String traceId, String path, String method,
                                 String query, int status, int latencyMs,
                                 String clientIp, String userAgent, String referer,
                                 Long loginUserId) {
        try {
            ApiAccessLogAddRequest request = new ApiAccessLogAddRequest();
            request.setTraceId(traceId);
            if (loginUserId != null) {
                request.setUserId(loginUserId);
            }
            request.setMethod(method);
            request.setPath(path);
            request.setQuery(query);
            request.setStatus(status);
            request.setLatencyMs(latencyMs);
            request.setClientIp(clientIp);
            request.setUserAgent(userAgent);
            request.setReferer(referer);

            logWebClient.post()
                    .uri(GatewayConstant.LOG_SERVICE_ACCESS_URI)
                    .header(SecurityConstant.FROM_SOURCE, SecurityConstant.INNER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(e -> {
                        logErrorThrottled("[Log] 上报访问日志失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            logErrorThrottled("[Log] 构建访问日志请求失败: {}", e.getMessage());
        }
    }

    /**
     * 限频错误日志输出，避免日志服务不可用时产生大量重复错误日志
     */
    private void logErrorThrottled(String format, String message) {
        long now = System.currentTimeMillis();
        long last = lastErrorLogTime.get();
        if (now - last > ERROR_LOG_INTERVAL_MS && lastErrorLogTime.compareAndSet(last, now)) {
            log.error(format, message);
        }
    }

    @Override
    public int getOrder() {
        // 在 GlobalHeaderSanitizeFilter (HIGHEST_PRECEDENCE) 之后，GlobalAuthFilter (-98) 之前
        return -200;
    }
}

