package com.example.webflux.service;

import com.example.webflux.model.ProductDoc;
import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Interfaz de servicio unificada para la gestión de productos.
 * Optimizada para JDK 11 y Spring WebFlux.
 */
public interface ProductService {

    // --- Consultas de Lectura ---

    Flux<ProductoResponse> findDisponibles();

    Flux<ProductoResponse> findAllActivos();

    Mono<ProductoResponse> findById(String id);

    Flux<ProductoResponse> findByNombre(String nombre);

    Flux<ProductoResponse> findByCategoria(String categoria);

    Flux<ProductoResponse> findByPrecioRange(BigDecimal precioMin, BigDecimal precioMax);

    // --- Operaciones de Escritura (Individual) ---

    Mono<ProductoResponse> create(ProductoRequest req);

    /**
     * CORRECCIÓN: El método debe recibir ProductoRequest para persistencia.
     * Retorna la entidad para uso interno o composición.
     */
    Mono<Producto> save(ProductoResponse request);

    Mono<ProductoResponse> update(String id, ProductoRequest req);

    Mono<ProductoResponse> patch(String id, ProductoRequest req);

    /**
     * CORRECCIÓN: Retorno Mono<Void> para operaciones de borrado.
     */
    Mono<Void> softDelete(String id);

    // --- Operaciones Bulk (Masivas) ---

    Flux<Producto> saveAll(Collection<Producto> productos);

    Flux<Producto> updateAll(Collection<Producto> productos);

    Flux<Producto> updateActivoAll(Collection<String> ids, Boolean activo);

    Flux<Producto> deactivateAll(Collection<String> ids);

    Mono<Long> deleteAllById(Collection<String> ids);

    // --- Operaciones de Conteo (Escalares) ---

    Mono<Long> countActivos();

    Mono<Long> countByActivo(boolean activo);

    // --- Operaciones Atómicas y Stock ---

    /**
     * Operación atómica sobre stock. Retorna el estado final.
     */
    Mono<ProductoResponse> reducirStock(@NotBlank String id, @Min(1) Integer cantidad);

    /**
     * Operación de bajo nivel con ReactiveMongoTemplate.
     */
    Mono<ProductDoc> updateAtomic(String id, ProductDoc r);
}