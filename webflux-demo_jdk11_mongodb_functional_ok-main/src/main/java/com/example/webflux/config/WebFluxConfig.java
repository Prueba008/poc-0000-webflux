package com.example.webflux.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class WebFluxConfig {

    @Value("${app.correlation.header:X-Correlation-Id}")
    private String correlationHeader;

    @Bean
    public WebProperties.Resources resources() {
        return new WebProperties.Resources();
    }
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000"
        ));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Origin","Content-Type","Accept","Authorization","X-Requested-With","Cache-Control", correlationHeader
        ));
        corsConfig.setExposedHeaders(Arrays.asList("Location", HttpHeaders.CONTENT_DISPOSITION, correlationHeader));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    // Deja el request tal cual; el correlation-id lo agrega el filtro WebFlux en headers de entrada.
                    return reactor.core.publisher.Mono.just(req);
                }))
                .build();
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name:webflux-demo}") String app,
            @Value("${spring.profiles.active:dev}") String env
    ) {
        return registry -> registry.config()
                .commonTags("application", app, "env", env)
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.buffer."));
    }
}
