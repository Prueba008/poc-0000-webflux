package com.example.webflux.controller;

import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.ErrorDetail;
import com.example.webflux.model.dto.bulk.BulkCreateRequest;
import com.example.webflux.model.dto.bulk.BulkOperationResult;
import com.example.webflux.model.dto.bulk.BulkStockUpdate;
import com.example.webflux.model.dto.bulk.BulkUpdateRequest;
import com.example.webflux.service.ProductService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v2/productos/bulk")
@RequiredArgsConstructor
public class ProductoBulkControllerV2 {

    private final ProductService productoService;

    /**
     * Creación masiva.
     * Implementación optimizada con Streams de Java 11 para mapeo de entidades.
     */
    // Bulk create: Corregido para retornar Mono con lista de IDs o conteo
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<Producto> bulkCreate(@RequestBody List<Producto> productos) {
        return productoService.saveAll(productos); //
    }

    // Bulk delete: Asegura que el flujo se complete correctamente
    @DeleteMapping("/bulk")
    public Mono<ResponseEntity<Void>> bulkDelete(@RequestBody List<String> ids) {
        return productoService.deactivateAll(ids) //
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    /**
     * Actualización masiva con control de concurrencia.
     * Utiliza ItemResult para gestionar éxitos y errores de forma granular sin detener el flujo.
     */
    @PutMapping("/update")
    public Mono<ResponseEntity<BulkOperationResult>> updateBulk(@Valid @RequestBody BulkUpdateRequest request) {
        final int CONCURRENCY = 32;

        return Flux.fromIterable(request.getProductos())
                .flatMap(dto -> {
                    if (dto.getId() == null) {
                        return Mono.just(ItemResult.fail(null, "ID requerido"));
                    }

                    return productoService.findById(dto.getId())
                            .flatMap(existing -> {
                                // Mapeo manual parcial (Patch-style)
                                if (dto.getNombre() != null) existing.setNombre(dto.getNombre());
                                if (dto.getPrecio() != null) existing.setPrecio(dto.getPrecio());
                                if (dto.getStock() != null) existing.setStock(dto.getStock());
                                existing.setFechaActualizacion(Instant.now());
                                return productoService.save(existing);
                            })
                            .map(saved -> ItemResult.ok(saved.getId()))
                            .onErrorResume(e -> Mono.just(ItemResult.fail(dto.getId(), e.getMessage())));
                }, CONCURRENCY)
                .collectList()
                .map(items -> {
                    List<String> okIds = items.stream()
                            .filter(ItemResult::isOk)
                            .map(ItemResult::getId)
                            .collect(Collectors.toList());

                    List<ErrorDetail> errs = items.stream()
                            .filter(it -> !it.isOk())
                            .map(it -> ErrorDetail.builder().id(it.getId()).message(it.getError()).build())
                            .collect(Collectors.toList());

                    return ResponseEntity.ok(BulkOperationResult.builder()
                            .operation("UPDATE")
                            .successCount(okIds.size())
                            .failedCount(items.size() - okIds.size())
                            .successIds(okIds)
                            .errors(errs)
                            .build());
                });
    }

    /**
     * Clase estática auxiliar para recolectar estados de procesamiento.
     */
    @Getter
    private static class ItemResult {
        private final boolean ok;
        private final String id;
        private final String error;

        private ItemResult(boolean ok, String id, String error) {
            this.ok = ok;
            this.id = id;
            this.error = error;
        }

        static ItemResult ok(String id) { return new ItemResult(true, id, null); }
        static ItemResult fail(String id, String error) { return new ItemResult(false, id, error); }
    }
}