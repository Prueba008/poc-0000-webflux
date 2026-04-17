package com.example.webflux.service;

import com.example.webflux.model.Producto;
import com.example.webflux.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Operations Service Tests")
class BulkOperationsServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Producto testProducto1;
    private Producto testProducto2;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        testProducto1 = Producto.builder()
                .id("product-1")
                .nombre("Product 1")
                .precio(new BigDecimal("100.00"))
                .stock(20)
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        testProducto2 = Producto.builder()
                .id("product-2")
                .nombre("Product 2")
                .precio(new BigDecimal("200.00"))
                .stock(30)
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
    }

    @Test
    @DisplayName("Should save multiple products with metadata")
    void saveAll_whenValidProducts_savesWithMetadata() {
        List<Producto> products = Arrays.asList(testProducto1, testProducto2);
        List<Producto> savedProducts = Arrays.asList(testProducto1, testProducto2);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.fromIterable(savedProducts));

        Flux<Producto> result = productService.saveAll(products);

        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo("product-1");
                    assertThat(product.getFechaCreacion()).isNotNull();
                    assertThat(product.getFechaActualizacion()).isNotNull();
                    assertThat(product.getActivo()).isTrue();
                })
                .assertNext(product -> {
                    assertThat(product.getId()).isEqualTo("product-2");
                    assertThat(product.getFechaCreacion()).isNotNull();
                    assertThat(product.getFechaActualizacion()).isNotNull();
                    assertThat(product.getActivo()).isTrue();
                })
                .verifyComplete();

        verify(productoRepository).saveAll(products);
    }

    @Test
    @DisplayName("Should handle empty bulk save")
    void saveAll_whenEmptyList_returnsEmptyFlux() {
        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.empty());

        Flux<Producto> result = productService.saveAll(Collections.emptyList());

        StepVerifier.create(result)
                .verifyComplete();

        verify(productoRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle bulk save errors gracefully")
    void saveAll_whenRepositoryError_propagatesError() {
        List<Producto> products = Collections.singletonList(testProducto1);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        Flux<Producto> result = productService.saveAll(products);

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RuntimeException.class);
                    assertThat(error).hasMessage("Database error");
                })
                .verify();

        verify(productoRepository).saveAll(products);
    }

    @Test
    @DisplayName("Should update multiple products successfully")
    void updateAll_whenValidProducts_updatesAllSuccessfully() {
        List<Producto> products = Arrays.asList(testProducto1, testProducto2);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.fromIterable(products));

        Flux<Producto> result = productService.updateAll(products);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(productoRepository).saveAll(products);
    }

    @Test
    @DisplayName("Should update active status for multiple products")
    void updateActivoAll_whenValidIds_updatesActiveStatus() {
        List<String> ids = Arrays.asList("product-1", "product-2");

        when(productoRepository.findAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Flux.just(testProducto1, testProducto2));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Flux<Producto> result = productService.updateActivoAll(ids, false);

        StepVerifier.create(result)
                .assertNext(product -> assertThat(product.getActivo()).isFalse())
                .assertNext(product -> assertThat(product.getActivo()).isFalse())
                .verifyComplete();

        verify(productoRepository).findAllById(ids);
        verify(productoRepository, times(2)).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should handle deactivate all operation")
    void deactivateAll_whenValidIds_deactivatesAllProducts() {
        List<String> ids = Arrays.asList("product-1", "product-2");

        when(productoRepository.findAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Flux.just(testProducto1, testProducto2));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Flux<Producto> result = productService.deactivateAll(ids);

        StepVerifier.create(result)
                .assertNext(product -> assertThat(product.getActivo()).isFalse())
                .assertNext(product -> assertThat(product.getActivo()).isFalse())
                .verifyComplete();

        verify(productoRepository).findAllById(ids);
        verify(productoRepository, times(2)).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should delete multiple products by ID")
    void deleteAllById_whenValidIds_deletesAndReturnsCount() {
        List<String> ids = Arrays.asList("product-1", "product-2");

        when(productoRepository.deleteAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Mono.empty());

        Mono<Long> result = productService.deleteAllById(ids);

        StepVerifier.create(result)
                .expectNext(2L)
                .verifyComplete();

        verify(productoRepository).deleteAllById(ids);
    }

    @Test
    @DisplayName("Should handle empty delete operation")
    void deleteAllById_whenEmptyList_returnsZeroCount() {
        when(productoRepository.deleteAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Mono.empty());

        Mono<Long> result = productService.deleteAllById(Collections.emptyList());

        StepVerifier.create(result)
                .expectNext(0L)
                .verifyComplete();

        verify(productoRepository).deleteAllById(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle delete errors gracefully")
    void deleteAllById_whenRepositoryError_propagatesError() {
        List<String> ids = Collections.singletonList("product-1");

        when(productoRepository.deleteAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        Mono<Long> result = productService.deleteAllById(ids);

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(RuntimeException.class);
                    assertThat(error).hasMessage("Delete failed");
                })
                .verify();

        verify(productoRepository).deleteAllById(ids);
    }

    @Test
    @DisplayName("Should handle concurrent bulk operations")
    void bulkOperations_shouldHandleConcurrentRequests() {
        List<Producto> products1 = Collections.singletonList(testProducto1);
        List<Producto> products2 = Collections.singletonList(testProducto2);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.just(testProducto1))
                .thenReturn(Flux.just(testProducto2));

        Flux<Producto> result1 = productService.saveAll(products1);
        Flux<Producto> result2 = productService.saveAll(products2);

        StepVerifier.create(Flux.merge(result1, result2))
                .expectNextCount(2)
                .verifyComplete();

        verify(productoRepository, times(2)).saveAll(Mockito.<Iterable<Producto>>any());
    }

    @Test
    @DisplayName("Should maintain performance with large bulk operations")
    void saveAll_withLargeList_shouldMaintainPerformance() {
        List<Producto> largeProductList = createLargeProductList(100);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.fromIterable(largeProductList));

        Flux<Producto> result = productService.saveAll(largeProductList);

        StepVerifier.create(result)
                .expectNextCount(100)
                .verifyComplete();

        verify(productoRepository).saveAll(largeProductList);
    }

    @Test
    @DisplayName("Should handle null collections gracefully")
    void bulkOperations_whenNullCollection_handlesGracefully() {
        Flux<Producto> result = productService.saveAll(null);

        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle partial failures in bulk operations")
    void updateActivoAll_whenSomeProductsNotFound_handlesPartialFailure() {
        List<String> ids = Arrays.asList("product-1", "non-existent-id");

        when(productoRepository.findAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Flux.just(testProducto1));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Flux<Producto> result = productService.updateActivoAll(ids, false);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(productoRepository).findAllById(ids);
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should maintain reactive chain in complex bulk operations")
    void complexBulkOperation_shouldMaintainReactiveChain() {
        List<Producto> products = Arrays.asList(testProducto1, testProducto2);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.fromIterable(products));

        when(productoRepository.findAllById(Mockito.<Iterable<String>>any()))
                .thenReturn(Flux.fromIterable(products));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Long> result = productService.saveAll(products)
                .map(Producto::getId)
                .collectList()
                .flatMapMany(ids -> productService.updateActivoAll(ids, false))
                .count();

        StepVerifier.create(result)
                .expectNext(2L)
                .verifyComplete();

        verify(productoRepository).saveAll(products);
        verify(productoRepository).findAllById(Arrays.asList("product-1", "product-2"));
        verify(productoRepository, times(2)).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should handle backpressure in bulk operations")
    void bulkOperations_shouldHandleBackpressure() {
        List<Producto> largeList = createLargeProductList(1000);

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.fromIterable(largeList));

        Flux<Producto> result = productService.saveAll(largeList).take(10);

        StepVerifier.create(result)
                .expectNextCount(10)
                .verifyComplete();

        verify(productoRepository).saveAll(largeList);
    }

    @Test
    @DisplayName("Should set default values during bulk save")
    void saveAll_shouldSetDefaultValuesForMissingFields() {
        Producto incompleteProduct = Producto.builder()
                .nombre("Incomplete Product")
                .precio(new BigDecimal("50.00"))
                .build();

        List<Producto> products = Collections.singletonList(incompleteProduct);

        Producto savedProduct = Producto.builder()
                .id("generated-id")
                .nombre("Incomplete Product")
                .precio(new BigDecimal("50.00"))
                .activo(true)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        when(productoRepository.saveAll(Mockito.<Iterable<Producto>>any()))
                .thenReturn(Flux.just(savedProduct));

        Flux<Producto> result = productService.saveAll(products);

        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getActivo()).isTrue();
                    assertThat(product.getFechaCreacion()).isNotNull();
                    assertThat(product.getFechaActualizacion()).isNotNull();
                })
                .verifyComplete();

        verify(productoRepository).saveAll(argThat((Iterable<Producto> iterable) -> {
            List<Producto> list = toList(iterable);
            return list.size() == 1
                    && list.get(0).getActivo() != null
                    && list.get(0).getFechaCreacion() != null
                    && list.get(0).getFechaActualizacion() != null;
        }));
    }

    @Test
    @DisplayName("Should count products by active status")
    void countByActivo_shouldReturnCorrectCount() {
        when(productoRepository.countByActivo(true)).thenReturn(Mono.just(15L));
        when(productoRepository.countByActivo(false)).thenReturn(Mono.just(5L));

        Mono<Long> activeCount = productService.countByActivo(true);
        Mono<Long> inactiveCount = productService.countByActivo(false);

        StepVerifier.create(activeCount)
                .expectNext(15L)
                .verifyComplete();

        StepVerifier.create(inactiveCount)
                .expectNext(5L)
                .verifyComplete();

        verify(productoRepository).countByActivo(true);
        verify(productoRepository).countByActivo(false);
    }

    private List<Producto> createLargeProductList(int size) {
        Instant now = Instant.now();

        return IntStream.range(0, size)
                .mapToObj(i -> Producto.builder()
                        .id("product-" + i)
                        .nombre("Product " + i)
                        .precio(new BigDecimal("100.00"))
                        .stock(10)
                        .activo(true)
                        .fechaCreacion(now)
                        .fechaActualizacion(now)
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        return iterable == null
                ? Collections.emptyList()
                : StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }
}