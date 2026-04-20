package com.example.webflux.service;

import com.example.webflux.exception.BusinessException;
import com.example.webflux.model.Producto;
import com.example.webflux.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceV2 implements ProductoServiceImplV2 {

    private final ProductoRepository productoRepository;

    @Override
    public Mono<Producto> save(Producto producto) {
        return Mono.justOrEmpty(producto)
                .switchIfEmpty(Mono.error(new BusinessException.BadRequest("El producto es obligatorio")))
                .flatMap(this::validateProducto)
                .map(p -> {
                    if (p.getFechaCreacion() == null) p.setFechaCreacion(Instant.now());
                    p.setFechaActualizacion(Instant.now());
                    return p;
                })
                .flatMap(productoRepository::save)
                .doOnError(e -> log.error("Error al guardar producto: {}", e.getMessage()));
    }

    @Override
    public Flux<Producto> saveAll(Collection<Producto> productos) {
        if (productos == null) {
            return Flux.error(new BusinessException.BadRequest("La colección de productos es obligatoria"));
        }

        return Flux.fromIterable(productos)
                .map(p -> {
                    p.setId(null);
                    if (p.getActivo() == null) p.setActivo(true);
                    return p;
                })
                .flatMap(this::save);
    }

    @Override
    public Mono<Long> deleteAllById(Collection<String> ids) {
        return productoRepository.deleteAllById(ids)
                .then(Mono.fromSupplier(() -> (long) ids.size()))
                .doOnSuccess(count -> log.info("Solicitada eliminación de {} productos", count));
    }

    @Override
    public Flux<Producto> updateStockAll(Collection<StockUpdateItem> stockUpdates) {
        // Convertimos a Mapa para acceso O(1) durante el flujo reactivo
        Map<String, Integer> updatesMap = stockUpdates.stream()
                .collect(Collectors.toMap(
                    StockUpdateItem::getProductoId, 
                    StockUpdateItem::getCantidad
                ));

        return productoRepository.findAllById(updatesMap.keySet())
                .flatMap(producto -> {
                    producto.setStock(updatesMap.get(producto.getId()));
                    producto.setFechaActualizacion(Instant.now());
                    return save(producto);
                });
    }

    @Override
    public Flux<Producto> findByCategoria(String categoria) {
        return productoRepository.findByCategoriaIgnoreCase(categoria);
    }

}