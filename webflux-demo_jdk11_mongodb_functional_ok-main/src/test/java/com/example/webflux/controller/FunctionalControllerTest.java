package com.example.webflux.controller;

import com.example.webflux.model.dto.ProductoRequest;
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
class FunctionalControllerTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    WebTestClient webTestClient;

    @Test
    void createViaFunctionalRoute_ok() {
        ProductoRequest req = ProductoRequest.builder()
                .nombre("Teclado Mecánico")
                .descripcion("Switches")
                .precio(new BigDecimal("79.90"))
                .stock(5)
                .categoria("Computación")
                .activo(true)
                .build();

        webTestClient.post()
                .uri("/api/v2/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Teclado Mecánico");
    }
}
