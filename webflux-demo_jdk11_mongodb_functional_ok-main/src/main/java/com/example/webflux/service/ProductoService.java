package com.example.webflux.service;

import com.example.webflux.exception.ConflictException;
import com.example.webflux.exception.NotFoundException;
import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.ProductoRequest;
import com.example.webflux.model.dto.ProductoResponse;
import com.example.webflux.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;

    public Flux<ProductoResponse> findAllActivos() {
        return repo.findByActivoTrue()
                .map(ProductoResponse::fromEntity);
    }

    public Mono<ProductoResponse> findById(String id) {
        return repo.findById(id)
                .filter(Producto::isDisponible)
                .map(ProductoResponse::fromEntity)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado o no disponible")));
    }

    public Flux<ProductoResponse> findDisponibles() {
        return repo.findByActivoTrueAndStockGreaterThan(0)
                .map(ProductoResponse::fromEntity);
    }

    public Flux<ProductoResponse> findByNombre(String nombre) {
        return repo.findByNombreContainingIgnoreCase(nombre)
                .filter(Producto::isDisponible)
                .map(ProductoResponse::fromEntity);
    }

    public Flux<ProductoResponse> findByCategoria(String categoria) {
        return repo.findByCategoriaIgnoreCase(categoria)
                .filter(Producto::isDisponible)
                .map(ProductoResponse::fromEntity);
    }

    public Flux<ProductoResponse> findByPrecioRange(java.math.BigDecimal min, java.math.BigDecimal max) {
        return repo.findByPrecioBetweenAndActivoTrue(min, max)
                .filter(Producto::isDisponible)
                .map(ProductoResponse::fromEntity);
    }

    public Mono<ProductoResponse> create(ProductoRequest req) {
        return repo.findByNombreIgnoreCase(req.getNombre())
                .flatMap(existing -> Mono.<Producto>error(new ConflictException("Ya existe un producto con ese nombre")))
                .switchIfEmpty(Mono.defer(() -> {
                    Producto p = Producto.builder()
                            .nombre(req.getNombre())
                            .descripcion(req.getDescripcion())
                            .precio(req.getPrecio())
                            .stock(req.getStock())
                            .categoria(req.getCategoria())
                            .activo(req.getActivo() != null ? req.getActivo() : Boolean.TRUE)
                            .fechaCreacion(Instant.now())
                            .fechaActualizacion(Instant.now())
                            .build();
                    return repo.save(p);
                }))
                .map(ProductoResponse::fromEntity)
                .doOnSuccess(p -> log.info("Producto creado: {}", p.getNombre()));
    }

    public Mono<ProductoResponse> update(String id, ProductoRequest req) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado")))
                .flatMap(existing -> {
                    existing.setNombre(req.getNombre());
                    existing.setDescripcion(req.getDescripcion());
                    existing.setPrecio(req.getPrecio());
                    existing.setStock(req.getStock());
                    existing.setCategoria(req.getCategoria());
                    existing.setActivo(req.getActivo());
                    existing.setFechaActualizacion(Instant.now());
                    return repo.save(existing);
                })
                .map(ProductoResponse::fromEntity);
    }

    public Mono<ProductoResponse> patch(String id, ProductoRequest req) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado")))
                .flatMap(existing -> {
                    if (req.getNombre() != null) existing.setNombre(req.getNombre());
                    if (req.getDescripcion() != null) existing.setDescripcion(req.getDescripcion());
                    if (req.getPrecio() != null) existing.setPrecio(req.getPrecio());
                    if (req.getStock() != null) existing.setStock(req.getStock());
                    if (req.getCategoria() != null) existing.setCategoria(req.getCategoria());
                    if (req.getActivo() != null) existing.setActivo(req.getActivo());
                    existing.setFechaActualizacion(Instant.now());
                    return repo.save(existing);
                })
                .map(ProductoResponse::fromEntity);
    }

    public Mono<Void> softDelete(String id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado")))
                .flatMap(p -> {
                    p.setActivo(false);
                    p.setFechaActualizacion(Instant.now());
                    return repo.save(p);
                })
                .then();
    }

    public Mono<Boolean> reducirStock(String id, int cantidad) {
        if (cantidad <= 0) return Mono.just(false);

        return repo.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado")))
                .flatMap(p -> {
                    int current = p.getStock() == null ? 0 : p.getStock();
                    if (current < cantidad) return Mono.just(false);
                    p.setStock(current - cantidad);
                    p.setFechaActualizacion(Instant.now());
                    return repo.save(p).thenReturn(true);
                });
    }

    public Mono<Long> countActivos() {
        return repo.findByActivoTrue().count();
    }
}
