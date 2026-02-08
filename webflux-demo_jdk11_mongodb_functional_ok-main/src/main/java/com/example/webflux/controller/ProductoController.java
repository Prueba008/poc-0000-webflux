package com.example.webflux.controller;

import com.example.webflux.model.dto.ProductoRequest;
import com.example.webflux.model.dto.ProductoResponse;
import com.example.webflux.service.ProductoService;
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

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductoResponse> getAll() {
        return service.findAllActivos();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> getById(@PathVariable @NotBlank String id) {
        return service.findById(id).map(ResponseEntity::ok);
    }

    @GetMapping(value = "/disponibles", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductoResponse> disponibles() {
        return service.findDisponibles();
    }

    @GetMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductoResponse> buscar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax
    ) {
        if (nombre != null) return service.findByNombre(nombre);
        if (categoria != null) return service.findByCategoria(categoria);
        if (precioMin != null && precioMax != null) return service.findByPrecioRange(precioMin, precioMax);
        return service.findAllActivos();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ProductoResponse>> create(@Valid @RequestBody ProductoRequest req) {
        return service.create(req)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> update(@PathVariable @NotBlank String id,
                                                        @Valid @RequestBody ProductoRequest req) {
        return service.update(id, req).map(ResponseEntity::ok);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ProductoResponse>> patch(@PathVariable @NotBlank String id,
                                                       @RequestBody ProductoRequest req) {
        return service.patch(id, req).map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> delete(@PathVariable @NotBlank String id) {
        return service.softDelete(id).thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/reducir-stock")
    public Mono<ResponseEntity<Boolean>> reducirStock(@PathVariable @NotBlank String id,
                                                      @RequestParam @Min(1) Integer cantidad) {
        return service.reducirStock(id, cantidad).map(ResponseEntity::ok);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductoResponse> stream() {
        return service.findAllActivos()
                .delayElements(Duration.ofSeconds(2));
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<Long>> count() {
        return service.countActivos().map(ResponseEntity::ok);
    }
}
