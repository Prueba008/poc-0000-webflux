package com.example.webflux.controller;

import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.bulk.BulkOperationResult;
import com.example.webflux.model.dto.bulk.BulkUpdateRequest;
import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import com.example.webflux.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoBulkControllerV2Test {

    @Mock
    private ProductService productService;

    private ProductoBulkControllerV2 controller;

    @BeforeEach
    void setUp() {
        controller = new ProductoBulkControllerV2(productService);
    }

    @Test
    void updateBulkWithNullRequestReturnsEmptySuccessfulResult() {
        ResponseEntity<BulkOperationResult> response = controller.updateBulk(null).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOperation()).isEqualTo("BULK_UPDATE");
        assertThat(response.getBody().getSuccessCount()).isZero();
        assertThat(response.getBody().getFailedCount()).isZero();
        assertThat(response.getBody().getSuccessIds()).isEmpty();
        assertThat(response.getBody().getErrors()).isEmpty();
        verify(productService, never()).patch(any(), any());
    }

    @Test
    void updateBulkWithNullProductsReturnsEmptySuccessfulResult() {
        ResponseEntity<BulkOperationResult> response = controller.updateBulk(new BulkUpdateRequest(null)).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessCount()).isZero();
        assertThat(response.getBody().getFailedCount()).isZero();
        verify(productService, never()).patch(any(), any());
    }

    @Test
    void updateBulkWithMissingIdsReturnsFailuresWithoutCallingService() {
        BulkUpdateRequest request = new BulkUpdateRequest(Arrays.asList(
                Producto.builder().id(null).nombre("Sin ID").build(),
                Producto.builder().id("   ").nombre("ID vacío").build()
        ));

        ResponseEntity<BulkOperationResult> response = controller.updateBulk(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessCount()).isZero();
        assertThat(response.getBody().getFailedCount()).isEqualTo(2);
        assertThat(response.getBody().getErrors()).hasSize(2);
        assertThat(response.getBody().getErrors())
                .allMatch(error -> "ID requerido".equals(error.getMessage()));
        verify(productService, never()).patch(any(), any());
    }

    @Test
    void updateBulkMapsSuccessfulPatchToSuccessIds() {
        Producto dto = Producto.builder()
                .id("p-1")
                .nombre("Notebook")
                .descripcion("Equipo portátil")
                .precio(new BigDecimal("1999.99"))
                .stock(5)
                .categoria("Computación")
                .activo(true)
                .build();

        ProductoResponse saved = ProductoResponse.builder().id("p-1").build();
        when(productService.patch(eq("p-1"), any(ProductoRequest.class))).thenReturn(Mono.just(saved));

        ResponseEntity<BulkOperationResult> response = controller.updateBulk(
                new BulkUpdateRequest(Collections.singletonList(dto))
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessCount()).isEqualTo(1);
        assertThat(response.getBody().getFailedCount()).isZero();
        assertThat(response.getBody().getSuccessIds()).containsExactly("p-1");
        assertThat(response.getBody().getErrors()).isEmpty();

        ArgumentCaptor<ProductoRequest> requestCaptor = ArgumentCaptor.forClass(ProductoRequest.class);
        verify(productService).patch(eq("p-1"), requestCaptor.capture());
        ProductoRequest mappedRequest = requestCaptor.getValue();
        assertThat(mappedRequest.getNombre()).isEqualTo("Notebook");
        assertThat(mappedRequest.getDescripcion()).isEqualTo("Equipo portátil");
        assertThat(mappedRequest.getPrecio()).isEqualByComparingTo("1999.99");
        assertThat(mappedRequest.getStock()).isEqualTo(5);
        assertThat(mappedRequest.getCategoria()).isEqualTo("Computación");
        assertThat(mappedRequest.getActivo()).isTrue();
    }

    @Test
    void updateBulkConvertsServiceErrorsToPartialFailures() {
        Producto dto = Producto.builder().id("missing-id").nombre("No existe").build();
        when(productService.patch(eq("missing-id"), any(ProductoRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Producto no encontrado")));

        ResponseEntity<BulkOperationResult> response = controller.updateBulk(
                new BulkUpdateRequest(Collections.singletonList(dto))
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessCount()).isZero();
        assertThat(response.getBody().getFailedCount()).isEqualTo(1);
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getId()).isEqualTo("missing-id");
        assertThat(response.getBody().getErrors().get(0).getMessage()).isEqualTo("Producto no encontrado");
    }
}
