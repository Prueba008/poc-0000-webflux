package com.example.webflux.repository;

import com.example.webflux.model.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

@DataMongoTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@Tag("tc")
class ProductoRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Test
    void saveAndFindByNombreIgnoreCase(ProductoRepository repo) {
        Producto p = Producto.builder()
                .nombre("Laptop Test")
                .descripcion("Laptop de prueba")
                .precio(new BigDecimal("1000.00"))
                .stock(5)
                .categoria("Computación")
                .activo(true)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        StepVerifier.create(repo.deleteAll().then(repo.save(p)))
                .expectNextMatches(saved -> saved.getId() != null)
                .verifyComplete();

        StepVerifier.create(repo.findByNombreIgnoreCase("laptop test"))
                .expectNextMatches(found -> found.getNombre().equals("Laptop Test"))
                .verifyComplete();
    }
}
