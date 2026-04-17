package com.example.webflux.service;

import com.example.webflux.exception.BusinessException;
import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import com.example.webflux.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService following TDD principles.
 * Tests business logic, edge cases, and reactive behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    private Producto testProducto;
    private ProductoRequest testRequest;
    private ProductoResponse testResponse;

    @BeforeEach
    void setUp() {
        testProducto = Producto.builder()
                .id("test-id")
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(new BigDecimal("99.99"))
                .stock(10)
                .categoria("Test Category")
                .activo(true)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        testRequest = ProductoRequest.builder()
                .nombre("Test Product")
                .descripcion("Test Description")
                .precio(new BigDecimal("99.99"))
                .stock(10)
                .categoria("Test Category")
                .activo(true)
                .build();

        testResponse = ProductoResponse.fromEntity(testProducto);
    }

    // --- FIND OPERATIONS TESTS ---

    @Test
    @DisplayName("Should find product by ID when exists")
    void findById_whenProductExists_returnsProductResponse() {
        // Given
        when(productoRepository.findById("test-id")).thenReturn(Mono.just(testProducto));

        // When
        Mono<ProductoResponse> result = productService.findById("test-id");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("test-id");
                    assertThat(response.getNombre()).isEqualTo("Test Product");
                    assertThat(response.getPrecio()).isEqualTo(new BigDecimal("99.99"));
                })
                .verifyComplete();

        verify(productoRepository).findById("test-id");
    }

    @Test
    @DisplayName("Should throw NotFound when product ID doesn't exist")
    void findById_whenProductNotExists_throwsNotFoundException() {
        // Given
        when(productoRepository.findById("non-existent-id")).thenReturn(Mono.empty());

        // When
        Mono<ProductoResponse> result = productService.findById("non-existent-id");

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.NotFound &&
                    ex.getMessage().contains("Producto no encontrado: non-existent-id"))
                .verify();

        verify(productoRepository).findById("non-existent-id");
    }

    @Test
    @DisplayName("Should find all active products")
    void findAllActivos_returnsActiveProducts() {
        // Given
        List<Producto> activeProducts = Arrays.asList(testProducto, createTestProduct("Product 2"));
        when(productoRepository.findByActivoTrue()).thenReturn(Flux.fromIterable(activeProducts));

        // When
        Flux<ProductoResponse> result = productService.findAllActivos();

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(productoRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("Should find products by name containing search term")
    void findByNombre_whenSearchTermExists_returnsMatchingProducts() {
        // Given
        when(productoRepository.findByNombreContainingIgnoreCase("test"))
                .thenReturn(Flux.just(testProducto));

        // When
        Flux<ProductoResponse> result = productService.findByNombre("test");

        // Then
        StepVerifier.create(result)
                .assertNext(product -> 
                    assertThat(product.getNombre()).containsIgnoringCase("test"))
                .verifyComplete();

        verify(productoRepository).findByNombreContainingIgnoreCase("test");
    }

    @Test
    @DisplayName("Should find products by category")
    void findByCategoria_whenCategoryExists_returnsCategoryProducts() {
        // Given
        when(productoRepository.findByCategoriaIgnoreCase("electronics"))
                .thenReturn(Flux.just(testProducto));

        // When
        Flux<ProductoResponse> result = productService.findByCategoria("electronics");

        // Then
        StepVerifier.create(result)
                .assertNext(product -> 
                    assertThat(product.getCategoria()).isEqualTo("electronics"))
                .verifyComplete();

        verify(productoRepository).findByCategoriaIgnoreCase("electronics");
    }

    @Test
    @DisplayName("Should find products by price range")
    void findByPrecioRange_whenValidRange_returnsProductsInRange() {
        // Given
        BigDecimal min = new BigDecimal("50.00");
        BigDecimal max = new BigDecimal("150.00");
        when(productoRepository.findByPrecioBetweenAndActivoTrue(min, max))
                .thenReturn(Flux.just(testProducto));

        // When
        Flux<ProductoResponse> result = productService.findByPrecioRange(min, max);

        // Then
        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getPrecio()).isBetween(min, max);
                    assertThat(product.getActivo()).isTrue();
                })
                .verifyComplete();

        verify(productoRepository).findByPrecioBetweenAndActivoTrue(min, max);
    }

    // --- CREATE OPERATIONS TESTS ---

    @Test
    @DisplayName("Should create product when name doesn't exist")
    void create_whenNameNotExists_createsProductSuccessfully() {
        // Given
        ProductoRequest newRequest = ProductoRequest.builder()
                .nombre("New Product")
                .precio(new BigDecimal("49.99"))
                .stock(5)
                .activo(true)
                .build();

        when(productoRepository.findByNombreIgnoreCase("New Product")).thenReturn(Mono.empty());
        when(productoRepository.save(any(Producto.class))).thenReturn(Mono.just(testProducto));

        // When
        Mono<ProductoResponse> result = productService.create(newRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getNombre()).isEqualTo("Test Product");
                    assertThat(response.getPrecio()).isEqualTo(new BigDecimal("99.99"));
                })
                .verifyComplete();

        verify(productoRepository).findByNombreIgnoreCase("New Product");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should throw Conflict when product name already exists")
    void create_whenNameExists_throwsConflictException() {
        // Given
        when(productoRepository.findByNombreIgnoreCase("Test Product"))
                .thenReturn(Mono.just(testProducto));

        // When
        Mono<ProductoResponse> result = productService.create(testRequest);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.Conflict &&
                    ex.getMessage().contains("El nombre ya existe"))
                .verify();

        verify(productoRepository).findByNombreIgnoreCase("Test Product");
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should validate negative price during creation")
    void create_whenPriceNegative_throwsBadRequest() {
        // Given
        ProductoRequest invalidRequest = ProductoRequest.builder()
                .nombre("Invalid Product")
                .precio(new BigDecimal("-10.00"))
                .stock(5)
                .build();

        when(productoRepository.findByNombreIgnoreCase("Invalid Product")).thenReturn(Mono.empty());

        // When
        Mono<ProductoResponse> result = productService.create(invalidRequest);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.BadRequest &&
                    ex.getMessage().contains("El precio no puede ser negativo"))
                .verify();

        verify(productoRepository, never()).save(any());
    }

    // --- UPDATE OPERATIONS TESTS ---

    @Test
    @DisplayName("Should update product when exists")
    void update_whenProductExists_updatesSuccessfully() {
        // Given
        ProductoRequest updateRequest = ProductoRequest.builder()
                .nombre("Updated Product")
                .precio(new BigDecimal("149.99"))
                .stock(20)
                .build();

        Producto updatedProducto = Producto.builder()
                .id("test-id")
                .nombre("Updated Product")
                .precio(new BigDecimal("149.99"))
                .stock(20)
                .build();

        when(productoRepository.findById("test-id")).thenReturn(Mono.just(testProducto));
        when(productoRepository.save(any(Producto.class))).thenReturn(Mono.just(updatedProducto));

        // When
        Mono<ProductoResponse> result = productService.update("test-id", updateRequest);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getNombre()).isEqualTo("Updated Product");
                    assertThat(response.getPrecio()).isEqualTo(new BigDecimal("149.99"));
                    assertThat(response.getStock()).isEqualTo(20);
                })
                .verifyComplete();

        verify(productoRepository).findById("test-id");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should throw NotFound when updating non-existent product")
    void update_whenProductNotExists_throwsNotFoundException() {
        // Given
        when(productoRepository.findById("non-existent-id")).thenReturn(Mono.empty());

        // When
        Mono<ProductoResponse> result = productService.update("non-existent-id", testRequest);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.NotFound &&
                    ex.getMessage().contains("Producto no encontrado"))
                .verify();

        verify(productoRepository).findById("non-existent-id");
        verify(productoRepository, never()).save(any());
    }

    // --- STOCK OPERATIONS TESTS ---

    @Test
    @DisplayName("Should reduce stock when sufficient stock exists")
    void reducirStock_whenSufficientStock_reducesSuccessfully() {
        // Given
        Producto updatedProducto = Producto.builder()
                .id("test-id")
                .nombre("Test Product")
                .stock(5) // Reduced from 10
                .build();

        when(productoRepository.findById("test-id")).thenReturn(Mono.just(testProducto));
        when(productoRepository.save(any(Producto.class))).thenReturn(Mono.just(updatedProducto));

        // When
        Mono<ProductoResponse> result = productService.reducirStock("test-id", 5);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getStock()).isEqualTo(5))
                .verifyComplete();

        verify(productoRepository).findById("test-id");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Should throw BadRequest when insufficient stock")
    void reducirStock_whenInsufficientStock_throwsBadRequest() {
        // Given
        when(productoRepository.findById("test-id")).thenReturn(Mono.just(testProducto));

        // When
        Mono<ProductoResponse> result = productService.reducirStock("test-id", 15); // More than available

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.BadRequest &&
                    ex.getMessage().contains("Stock insuficiente"))
                .verify();

        verify(productoRepository).findById("test-id");
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFound when reducing stock of non-existent product")
    void reducirStock_whenProductNotExists_throwsNotFoundException() {
        // Given
        when(productoRepository.findById("non-existent-id")).thenReturn(Mono.empty());

        // When
        Mono<ProductoResponse> result = productService.reducirStock("non-existent-id", 5);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(ex -> 
                    ex instanceof BusinessException.NotFound &&
                    ex.getMessage().contains("Producto no encontrado"))
                .verify();

        verify(productoRepository).findById("non-existent-id");
        verify(productoRepository, never()).save(any());
    }

    // --- BULK OPERATIONS TESTS ---

    @Test
    @DisplayName("Should save multiple products successfully")
    void saveAll_whenValidProducts_savesAllSuccessfully() {
        // Given
        List<Producto> products = Arrays.asList(testProducto, createTestProduct("Product 2"));
        when(productoRepository.saveAll(Collections.singleton(any()))).thenReturn(Flux.fromIterable(products));

        // When
        Flux<Producto> result = productService.saveAll(products);

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(productoRepository).saveAll(products);
    }

    @Test
    @DisplayName("Should count active products")
    void countActivos_returnsActiveProductCount() {
        // Given
        when(productoRepository.countByActivo(true)).thenReturn(Mono.just(5L));

        // When
        Mono<Long> result = productService.countActivos();

        // Then
        StepVerifier.create(result)
                .expectNext(5L)
                .verifyComplete();

        verify(productoRepository).countByActivo(true);
    }

    @Test
    @DisplayName("Should soft delete product")
    void softDelete_whenProductExists_setsInactive() {
        // Given
        Producto inactiveProducto = Producto.builder()
                .id("test-id")
                .nombre("Test Product")
                .activo(false)
                .build();

        when(productoRepository.findById("test-id")).thenReturn(Mono.just(testProducto));
        when(productoRepository.save(any(Producto.class))).thenReturn(Mono.just(inactiveProducto));

        // When
        Mono<Void> result = productService.softDelete("test-id");

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(productoRepository).findById("test-id");
        verify(productoRepository).save(argThat(product -> !product.getActivo()));
    }

    // --- EDGE CASES AND ERROR HANDLING TESTS ---

    @Test
    @DisplayName("Should handle null ID gracefully in find operations")
    void findById_whenNullId_throwsAppropriateError() {
        // When
        Mono<ProductoResponse> result = productService.findById(null);

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should handle empty repository responses gracefully")
    void findAllActivos_whenNoProducts_returnsEmptyFlux() {
        // Given
        when(productoRepository.findByActivoTrue()).thenReturn(Flux.empty());

        // When
        Flux<ProductoResponse> result = productService.findAllActivos();

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();

        verify(productoRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("Should handle repository errors gracefully")
    void findById_whenRepositoryError_propagatesError() {
        // Given
        when(productoRepository.findById("test-id"))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When
        Mono<ProductoResponse> result = productService.findById("test-id");

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(productoRepository).findById("test-id");
    }

    // --- REACTIVE BEHAVIOR TESTS ---

    @Test
    @DisplayName("Should maintain reactive chain for complex operations")
    void complexOperation_shouldMaintainReactiveChain() {
        // Given
        when(productoRepository.findByNombreIgnoreCase("Test Product")).thenReturn(Mono.empty());
        when(productoRepository.save(any(Producto.class))).thenReturn(Mono.just(testProducto));

        // When
        Mono<ProductoResponse> result = productService.create(testRequest)
                .flatMap(response -> productService.findById(response.getId()));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getId()).isEqualTo("test-id"))
                .verifyComplete();

        verify(productoRepository).findByNombreIgnoreCase("Test Product");
        verify(productoRepository).save(any(Producto.class));
        verify(productoRepository).findById("test-id");
    }

    // Helper methods
    private Producto createTestProduct(String name) {
        return Producto.builder()
                .id(name + "-id")
                .nombre(name)
                .descripcion("Description for " + name)
                .precio(new BigDecimal("99.99"))
                .stock(10)
                .categoria("Test Category")
                .activo(true)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
    }
}
