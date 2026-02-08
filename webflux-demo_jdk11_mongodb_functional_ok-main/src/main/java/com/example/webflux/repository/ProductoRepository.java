package com.example.webflux.repository;

import com.example.webflux.model.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ProductoRepository extends ReactiveMongoRepository<Producto, String> {

    Flux<Producto> findByActivoTrue();

    Flux<Producto> findByNombreContainingIgnoreCase(String nombre);

    Flux<Producto> findByCategoriaIgnoreCase(String categoria);

    Mono<Producto> findByNombreIgnoreCase(String nombre);

    Flux<Producto> findByPrecioBetweenAndActivoTrue(BigDecimal min, BigDecimal max);

    Flux<Producto> findByActivoTrueAndStockGreaterThan(Integer stock);
}
