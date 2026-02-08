package com.example.webflux.observability;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdWebFilter implements WebFilter {

    public static final String CTX_KEY = "correlationId";

    @Value("${app.correlation.header:X-Correlation-Id}")
    private String headerName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();

        String incoming = req.getHeaders().getFirst(headerName);
        String correlationId = Optional.ofNullable(incoming).filter(s -> !s.isBlank()).orElse(UUID.randomUUID().toString());

        // Set response header
        exchange.getResponse().getHeaders().set(headerName, correlationId);

        // Put into MDC for logging (current thread) and into Reactor Context for propagation
        MDC.put(CTX_KEY, correlationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CTX_KEY, correlationId))
                .doFinally(sig -> MDC.remove(CTX_KEY));
    }
}
