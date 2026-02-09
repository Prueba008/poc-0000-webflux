package com.example.webflux.service;

import com.example.webflux.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collection;

public interface ProductoServiceImplV2 {
    Mono<Producto> save(Producto producto);

    Flux<Producto> saveAll(Collection<Producto> productos);

    Mono<Long> deleteAllById(Collection<String> ids);

    Flux<Producto> updateStockAll(Collection<StockUpdateItem> stockUpdates);

    Flux<Producto> findByCategoria(String categoria);

    // --- Helper de Validación Reactiva ---
    default Mono<Producto> validateProducto(Producto p) {
        if (p.getStock() != null && p.getStock() < 0) {
            return Mono.error(new IllegalArgumentException("El stock no puede ser negativo"));
        }
        if (p.getPrecio() != null && p.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new IllegalArgumentException("El precio no puede ser negativo"));
        }
        return Mono.just(p);
    }

    // --- Clase DTO interna adaptada a JDK 11 ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateItem {
        private String productoId;
        private Integer cantidad;
    }
}
