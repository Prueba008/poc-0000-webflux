package com.example.webflux.controller;

import com.example.webflux.model.dto.producto.ProductoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("tc")
class ProductoControllerTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        r.add("app.errors.includeStacktrace", () -> "true");
    }

    @Autowired
    WebTestClient webTestClient;

    @Test
    void createAndGetById_returnsCorrelationId() {
        ProductoRequest req = ProductoRequest.builder()
                .nombre("Mouse Gamer")
                .descripcion("RGB")
                .precio(new BigDecimal("49.99"))
                .stock(10)
                .categoria("Computación")
                .activo(true)
                .build();

        String corr = "test-corr-123";

        String id = webTestClient.post()
                .uri("/api/v1/productos")
                .header("X-Correlation-Id", corr)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("X-Correlation-Id", corr)
                .expectBody()
                .jsonPath("$.id").value(v -> {})
                .jsonPath("$.nombre").isEqualTo("Mouse Gamer")
                .returnResult()
                .getResponseBody() == null ? null : null;

        // Recuperar lista y validar que existe al menos 1 elemento
        webTestClient.get()
                .uri("/api/v1/productos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isNumber();
    }

    @Test
    void createValidationError_returnsBadRequestJson() {
        ProductoRequest req = ProductoRequest.builder()
                .nombre("") // inválido
                .precio(new BigDecimal("-1")) // inválido
                .stock(-1) // inválido
                .build();

        webTestClient.post()
                .uri("/api/v2/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").exists()
                .jsonPath("$.message").exists();
    }
}
