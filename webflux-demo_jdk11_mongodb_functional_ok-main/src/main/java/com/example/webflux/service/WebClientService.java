package com.example.webflux.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebClientService {

    private final WebClient webClient;

    public Mono<String> healthCheck() {
        return webClient.get()
                .uri("http://localhost:8080/actuator/health")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                // Maneja errores de respuesta (4xx, 5xx) antes de convertir el cuerpo
                .onStatus(HttpStatus::isError, response -> Mono.error(new RuntimeException("Health check failed")))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                // CORRECCIÓN: Escapar las comillas dobles con \
                .onErrorReturn("{\"status\":\"DOWN\"}");
    }
}
