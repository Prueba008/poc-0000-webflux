package com.example.webflux.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Configuración central de la infraestructura WebFlux, Seguridad y Observabilidad.
 */
@Configuration
public class WebFluxConfig {

    @Value("${app.correlation.header:X-Correlation-Id}")
    private String correlationHeader;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    /**
     * Configuración reactiva de CORS.
     * Utiliza orígenes inyectados desde variables de entorno para facilitar el despliegue en CI/CD.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(List.of(
                "Origin", "Content-Type", "Accept", "Authorization", correlationHeader
        ));
        corsConfig.setExposedHeaders(List.of("Location", HttpHeaders.CONTENT_DISPOSITION, correlationHeader));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }

    /**
     * Bean de WebClient reutilizable.
     * En JDK 11, WebClient es la opción preferida sobre RestTemplate por su naturaleza no bloqueante.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8080") // Opcional: configurar URL base
                .build();
    }

    /**
     * Personalización de métricas para Prometheus/Grafana.
     * Agrega tags comunes (aplicación y entorno) a todas las métricas registradas.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name:webflux-demo}") String app,
            @Value("${spring.profiles.active:dev}") String env
    ) {
        return registry -> registry.config()
                .commonTags("application", app, "env", env)
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.buffer."));
    }
    /**
     * Define el bean de recursos requerido por el GlobalErrorWebExceptionHandler.
     * En Spring Boot 2.7.x, este bean debe ser declarado explícitamente si se
     * personaliza el manejo de errores reactivos.
     */
    @Bean
    public WebProperties.Resources resources() {
        return new WebProperties.Resources();
    }
}