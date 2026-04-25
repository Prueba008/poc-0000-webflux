package com.example.webflux.controller;

import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.ErrorDetail;
import com.example.webflux.model.dto.bulk.BulkOperationResult;
import com.example.webflux.model.dto.bulk.BulkUpdateRequest;
import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.service.ProductService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Operaciones masivas optimizadas para alto rendimiento.
 * Utiliza concurrencia controlada para no saturar el pool de conexiones.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/productos/bulk")
@RequiredArgsConstructor
public class ProductoBulkControllerV2 {

    private static final int CONCURRENCY_LIMIT = 32;
    private static final String BULK_UPDATE_OPERATION = "BULK_UPDATE";

    private final ProductService productoService;

    /**
     * Actualiza un lote de productos de forma asíncrona.
     * Implementa un patrón de error parcial (Partial Failure) para resiliencia.
     */
    @PutMapping("/update")
    public Mono<ResponseEntity<BulkOperationResult>> updateBulk(@Valid @RequestBody(required = false) BulkUpdateRequest request) {
        List<Producto> productos = request == null || request.getProductos() == null
                ? Collections.emptyList()
                : request.getProductos();

        return Flux.fromIterable(productos)
                .flatMap(dto -> {
                    if (dto.getId() == null || dto.getId().isBlank()) {
                        return Mono.just(ItemResult.fail(dto.getId(), "ID requerido"));
                    }

                    ProductoRequest req = ProductoRequest.builder()
                            .nombre(dto.getNombre())
                            .descripcion(dto.getDescripcion())
                            .precio(dto.getPrecio())
                            .stock(dto.getStock())
                            .categoria(dto.getCategoria())
                            .activo(dto.getActivo())
                            .build();

                    return productoService.patch(dto.getId(), req)
                            .map(saved -> ItemResult.ok(saved.getId()))
                            .onErrorResume(e -> Mono.just(ItemResult.fail(dto.getId(), e.getMessage())));
                }, CONCURRENCY_LIMIT)
                .collectList()
                .map(this::mapToBulkResult);
    }

    private ResponseEntity<BulkOperationResult> mapToBulkResult(List<ItemResult> results) {
        List<String> successIds = results.stream()
                .filter(ItemResult::isOk)
                .map(ItemResult::getId)
                .collect(Collectors.toList());

        List<ErrorDetail> errors = results.stream()
                .filter(it -> !it.isOk())
                .map(it -> ErrorDetail.builder().id(it.getId()).message(it.getError()).build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(BulkOperationResult.builder()
                .operation(BULK_UPDATE_OPERATION)
                .successCount(successIds.size())
                .failedCount(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build());
    }

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
