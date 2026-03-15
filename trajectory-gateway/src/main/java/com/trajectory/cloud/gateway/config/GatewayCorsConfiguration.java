package com.trajectory.cloud.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 网关统一跨域配置
 * <p>
 * 通过编写 CorsWebFilter Bean 并设置最高优先级，确保所有请求（包括预检请求）
 * 以及所有响应（包括过滤器拦截返回的 401、异常处理器返回的 503 等）都能正确携带跨域头。
 * </p>
 *
 * @author StephenQiu30
 */
@Configuration
public class GatewayCorsConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许发送 Cookie
        config.setAllowCredentials(true);
        
        // 允许的源（生产环境建议配置具体域名）
        config.addAllowedOriginPattern("*");
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许的请求方法
        config.addAllowedMethod("*");
        
        // 预检请求有效期 (1小时)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
