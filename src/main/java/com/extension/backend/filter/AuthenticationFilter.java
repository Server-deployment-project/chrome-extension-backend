package com.extension.backend.filter;

import com.extension.backend.entity.User;
import com.extension.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 认证过滤器 - 验证 X-Extension-Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements WebFilter {

    private final UserService userService;

    // 不需要认证的路径
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/health",
            "/api/v1/register",
            "/api/v1/login",
            "/api/v1/reset-password"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 检查是否是公开路径
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return chain.filter(exchange);
            }
        }

        // 获取 Token
        String token = exchange.getRequest().getHeaders().getFirst("X-Extension-Token");

        if (token == null || token.isEmpty()) {
            log.warn("Missing X-Extension-Token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 验证 Token（使用 Schedulers.boundedElastic() 包装阻塞调用）
        return Mono.fromCallable(() -> userService.findByToken(token))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Invalid token (not found) for path: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                }))
                .flatMap(user -> {
                    if (!user.getIsActive()) {
                        log.warn("Inactive token for path: {}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    // 将用户信息存入上下文
                    exchange.getAttributes().put("USER", user);
                    log.debug("Authenticated user: {} for path: {}", user.getEmail(), path);

                    return chain.filter(exchange);
                })
                .onErrorResume(error -> {
                    log.error("Authentication error: ", error);
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }
}
