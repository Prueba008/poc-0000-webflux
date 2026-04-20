package com.example.webflux.service;

import com.example.webflux.exception.BusinessException;
import com.example.webflux.model.Producto;
import com.example.webflux.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoServiceV2 validations")
class ProductoServiceV2Test {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoServiceV2 productoService;

    @Test
    @DisplayName("save should fail when product is null")
    void save_whenProductIsNull_shouldReturnBusinessBadRequest() {
        StepVerifier.create(productoService.save(null))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.BadRequest.class);
                    assertThat(error).hasMessage("El producto es obligatorio");
                })
                .verify();
    }

    @Test
    @DisplayName("save should fail when stock is negative")
    void save_whenNegativeStock_shouldReturnBusinessBadRequest() {
        Producto producto = Producto.builder()
                .nombre("Producto test")
                .precio(new BigDecimal("10.00"))
                .stock(-1)
                .build();

        StepVerifier.create(productoService.save(producto))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(BusinessException.BadRequest.class);
                    assertThat(error).hasMessage("El stock no puede ser negativo");
                })
                .verify();
    }

    @Test
    @DisplayName("save should persist valid product with timestamps")
    void save_whenProductIsValid_shouldPersistWithTimestamps() {
        Producto producto = Producto.builder()
                .nombre("Producto válido")
                .precio(new BigDecimal("25.50"))
                .stock(5)
                .activo(true)
                .build();

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(productoService.save(producto))
                .assertNext(saved -> {
                    assertThat(saved.getNombre()).isEqualTo("Producto válido");
                    assertThat(saved.getFechaCreacion()).isNotNull();
                    assertThat(saved.getFechaActualizacion()).isNotNull();
                    assertThat(saved.getFechaActualizacion()).isAfterOrEqualTo(saved.getFechaCreacion());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("save should preserve existing creation date")
    void save_whenCreationDateExists_shouldKeepOriginalValue() {
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Producto producto = Producto.builder()
                .nombre("Producto con fecha")
                .precio(new BigDecimal("50.00"))
                .stock(8)
                .fechaCreacion(createdAt)
                .build();

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(productoService.save(producto))
                .assertNext(saved -> {
                    assertThat(saved.getFechaCreacion()).isEqualTo(createdAt);
                    assertThat(saved.getFechaActualizacion()).isNotNull();
                })
                .verifyComplete();
    }
}
