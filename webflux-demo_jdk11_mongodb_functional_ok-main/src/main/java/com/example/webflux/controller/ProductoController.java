package com.example.webflux.controller;

import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import com.example.webflux.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Duration;

/**
 * Endpoint principal para la gestión individual de productos.
 * Implementa un modelo de programación reactivo no bloqueante.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductService productoService;

    /**
     * Recupera el catálogo completo de productos en estado activo.
     * @return Flux con la secuencia de ProductoResponse.
     */
    @GetMapping("/activos")
    public Flux<ProductoResponse> getAll() {
        log.info("Iniciando recuperación de productos activos");
        return productoService.findAllActivos();
    }

    /**
     * Obtiene un producto por su identificador único.
     * @param id Identificador en formato String.
     * @return Mono con ResponseEntity (200 OK o 404 si no existe).
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> getById(@PathVariable @NotBlank String id) {
        return productoService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint polimórfico de búsqueda.
     * Prioriza por nombre, luego categoría y finalmente rango de precio.
     */
    @GetMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductoResponse> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax
    ) {
        if (nombre != null) return productoService.findByNombre(nombre);
        if (categoria != null) return productoService.findByCategoria(categoria);
        if (precioMin != null && precioMax != null) return productoService.findByPrecioRange(precioMin, precioMax);
        return productoService.findAllActivos();
    }

    /**
     * Actualización parcial de stock con validación de concurrencia.
     * @param id ID del producto.
     * @param cantidad Unidades a reducir (mínimo 1).
     */
    @PostMapping("/{id}/reducir-stock")
    public Mono<ResponseEntity<ProductoResponse>> reducirStock(
            @PathVariable @NotBlank String id,
            @RequestParam @Min(1) Integer cantidad) {
        
        log.info("Procesando reducción de stock: {} unidades para el ID {}", cantidad, id);
        return productoService.reducirStock(id, cantidad)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Fallo en reducción de stock: {}", e.getMessage()));
    }

    /**
     * Stream de productos en tiempo real mediante Server-Sent Events (SSE).
     * Útil para dashboards o monitoreo en vivo.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductoResponse> stream() {
        return productoService.findAllActivos()
                .delayElements(Duration.ofSeconds(2));
    }
}