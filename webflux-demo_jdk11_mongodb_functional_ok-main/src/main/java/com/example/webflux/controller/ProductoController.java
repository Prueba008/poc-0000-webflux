package com.example.webflux.controller;

import com.example.webflux.model.Producto;
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
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductService productoService;

    /**
     * Recupera todos los productos activos.
     * Se asegura la conversión de Entidad a DTO para mantener la integridad de la API.
     */
    @GetMapping("/activos")
    public Flux<ProductoResponse> getAll() {
        log.info("Petición recibida para listar todos los productos activos");
        return productoService.findAllActivos();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> getById(@PathVariable @NotBlank String id) {
        return productoService.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping(value = "/disponibles", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductoResponse> disponibles() {
        return productoService.findDisponibles();
    }

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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ProductoResponse>> create(@Valid @RequestBody ProductoRequest req) {
        return productoService.create(req)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> update(@PathVariable @NotBlank String id,
                                                        @Valid @RequestBody ProductoRequest req) {
        return productoService.update(id, req).map(ResponseEntity::ok);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> patch(@PathVariable @NotBlank String id,
                                                       @RequestBody ProductoRequest req) {
        return productoService.patch(id, req).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> delete(@PathVariable @NotBlank String id) {
        return productoService.softDelete(id).thenReturn(ResponseEntity.noContent().build());
    }
    @PostMapping("/{id}/reducir-stock")
    public Mono<ResponseEntity<ProductoResponse>> reducirStock(
            @PathVariable @NotBlank String id,
            @RequestParam @Min(1) Integer cantidad) {

        log.info("Solicitud de reducción de stock: Producto {}, Cantidad {}", id, cantidad);

        return productoService.reducirStock(id, cantidad)
                // .map(ProductoResponse::fromEntity) <-- ELIMINAR: Ya es un ProductoResponse
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Error al reducir stock: {}", e.getMessage()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductoResponse> stream() {
        return productoService.findAllActivos()
                .delayElements(Duration.ofSeconds(2));
    }

    /**
     * Retorna el conteo total de productos activos.
     * Se utiliza ResponseEntity para cumplir con los estándares REST.
     */
    @GetMapping("/count")
    public Mono<ResponseEntity<Long>> count() {
        log.info("Petición recibida para contar productos activos");
        return productoService.countByActivo(true)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Error al contar productos: {}", e.getMessage()));
    }

    //bulk
    // Bulk create: recibe una lista de productos y los guarda
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<Producto> bulkCreate(@RequestBody List<Producto> productos) {
        return productoService.saveAll(productos);
    }

    // Bulk update: recibe una lista de productos (con id) y los actualiza
    @PutMapping("/bulk")
    public Flux<Producto> bulkUpdate(@RequestBody List<Producto> productos) {
        return productoService.updateAll(productos);
    }

    // Bulk delete: recibe una lista de ids y los marca como inactivos (borrado lógico)
    @DeleteMapping("/bulk")
    public Mono<Void> bulkDelete(@RequestBody List<String> ids) {
        return productoService.deactivateAll(ids).then();
    }
}
