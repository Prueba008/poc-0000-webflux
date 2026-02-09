package com.example.webflux.repository;

import com.example.webflux.model.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductoRepository extends ReactiveMongoRepository<Producto, String> {

    Flux<Producto> findByActivoTrue();

    Flux<Producto> findByNombreContainingIgnoreCase(String nombre);

    Flux<Producto> findByCategoriaIgnoreCase(String categoria);

    Mono<Producto> findByNombreIgnoreCase(String nombre);

    Flux<Producto> findByPrecioBetweenAndActivoTrue(BigDecimal min, BigDecimal max);

    Flux<Producto> findByActivoTrueAndStockGreaterThan(Integer stock);

    Flux<Producto> findByCategoria(String categoria);

    Flux<Producto> findByActivo(Boolean activo);

    Flux<Producto> findByStockGreaterThan(Integer stock);

    Flux<Producto> findByPrecioBetween(BigDecimal min, BigDecimal max);

    @Query("{ 'nombre': { $regex: ?0, $options: 'i' } }")
    Flux<Producto> findByNombreContaining(String nombre);

    @Query("{ 'activo': true, 'stock': { $gt: 0 } }")
    Flux<Producto> findDisponibles();

    Flux<Producto> findAllByIdIn(List<String> ids);

    Mono<Long> countByCategoria(String categoria);

    Mono<Long> countByActivo(Boolean activo);

}
