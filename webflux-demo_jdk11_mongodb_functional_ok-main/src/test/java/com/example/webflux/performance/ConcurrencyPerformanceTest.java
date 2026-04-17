package com.example.webflux.performance;

import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("performance")
@DisplayName("Concurrency and Performance Tests")
class ConcurrencyPerformanceTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6");

    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ProductoRepository productoRepository;

    private WebClient webClient;

    @BeforeEach
    void setUp() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        productoRepository.deleteAll().block(Duration.ofSeconds(30));
        seedProducts(50, "seed-product-");
    }

    @Test
    @DisplayName("Should handle 100 concurrent read requests")
    void concurrentReadRequests_shouldHandleGracefully() {
        int concurrentRequests = 100;

        Mono<List<Integer>> execution = Flux.range(0, concurrentRequests)
                .flatMap(i -> getProductosStatus(), 25)
                .collectList();

        StepVerifier.create(execution)
                .assertNext(statuses -> {
                    assertThat(statuses).hasSize(concurrentRequests);
                    assertThat(statuses).allMatch(this::is2xx);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle 50 concurrent create requests")
    void concurrentCreateRequests_shouldHandleGracefully() {
        int concurrentRequests = 50;

        Mono<List<Integer>> execution = Flux.range(0, concurrentRequests)
                .flatMap(i -> createProductoStatus("concurrent-product-" + i), 20)
                .collectList();

        StepVerifier.create(execution)
                .assertNext(statuses -> {
                    assertThat(statuses).hasSize(concurrentRequests);
                    assertThat(statuses).allMatch(this::is2xx);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle mixed concurrent operations")
    void mixedConcurrentOperations_shouldMaintainConsistency() {
        int operationsPerType = 20;

        List<String> idsToUpdate = Flux.range(0, operationsPerType)
                .flatMap(i -> createProductoAndReturnId("updatable-product-" + i), 10)
                .collectList()
                .block(Duration.ofSeconds(30));

        assertThat(idsToUpdate).isNotNull();
        assertThat(idsToUpdate).hasSize(operationsPerType);

        Mono<List<Integer>> reads = Flux.range(0, operationsPerType)
                .flatMap(i -> getProductosStatus(), 10)
                .collectList();

        Mono<List<Integer>> creates = Flux.range(0, operationsPerType)
                .flatMap(i -> createProductoStatus("mixed-product-" + i), 10)
                .collectList();

        Mono<List<Integer>> updates = Flux.range(0, operationsPerType)
                .flatMap(i -> updateProductoStatus(idsToUpdate.get(i), buildProductoRequest("updated-product-" + i)), 10)
                .collectList();

        StepVerifier.create(Mono.zip(reads, creates, updates))
                .assertNext(tuple -> {
                    List<Integer> readStatuses = tuple.getT1();
                    List<Integer> createStatuses = tuple.getT2();
                    List<Integer> updateStatuses = tuple.getT3();

                    assertThat(readStatuses).hasSize(operationsPerType).allMatch(this::is2xx);
                    assertThat(createStatuses).hasSize(operationsPerType).allMatch(this::is2xx);
                    assertThat(updateStatuses).hasSize(operationsPerType).allMatch(this::is2xx);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle high-frequency streaming requests")
    void highFrequencyStreaming_shouldMaintainPerformance() {
        int concurrentStreams = 20;

        Mono<List<Boolean>> execution = Flux.range(0, concurrentStreams)
                .flatMap(i -> consumeFirstStreamEvent(), 10)
                .collectList();

        StepVerifier.create(execution)
                .assertNext(results -> {
                    assertThat(results).hasSize(concurrentStreams);
                    assertThat(results).allMatch(Boolean::booleanValue);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle bulk operations under load")
    void bulkOperationsUnderLoad_shouldMaintainPerformance() {
        int batchCount = 5;
        int bulkSize = 50;

        Mono<List<Integer>> execution = Flux.range(0, batchCount)
                .flatMap(batch ->
                                Flux.range(0, bulkSize)
                                        .flatMap(i -> createProductoStatus("bulk-product-" + batch + "-" + i), 10)
                                        .filter(this::is2xx)
                                        .count()
                                        .map(Long::intValue),
                        2
                )
                .collectList();

        StepVerifier.create(execution)
                .assertNext(batchSuccessCounts -> {
                    assertThat(batchSuccessCounts).hasSize(batchCount);
                    assertThat(batchSuccessCounts).allMatch(count -> count == bulkSize);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should meet performance benchmarks for read operations")
    void readOperations_shouldMeetPerformanceBenchmarks() {
        int requestCount = 200;
        long maxAverageResponseTimeMs = 300;

        List<Long> latencies = Flux.range(0, requestCount)
                .flatMap(i -> timedReadRequest(), 25)
                .collectList()
                .block(Duration.ofSeconds(60));

        assertThat(latencies).isNotNull();
        assertThat(latencies).hasSize(requestCount);

        double average = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElseThrow();

        assertThat(average).isLessThan(maxAverageResponseTimeMs);
    }

    @Test
    @DisplayName("Should meet performance benchmarks for create operations")
    void createOperations_shouldMeetPerformanceBenchmarks() {
        int requestCount = 100;
        long maxAverageResponseTimeMs = 500;

        List<Long> latencies = Flux.range(0, requestCount)
                .flatMap(i -> timedCreateRequest("benchmark-product-" + i), 15)
                .collectList()
                .block(Duration.ofSeconds(60));

        assertThat(latencies).isNotNull();
        assertThat(latencies).hasSize(requestCount);

        double average = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElseThrow();

        assertThat(average).isLessThan(maxAverageResponseTimeMs);
    }

    @Test
    @DisplayName("Should handle backpressure in streaming")
    void streaming_shouldHandleBackpressure() {
        int bufferSize = 10;

        StepVerifier.create(
                        webClient.get()
                                .uri("/api/v2/productos/stream")
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .retrieve()
                                .bodyToFlux(String.class)
                                .take(bufferSize)
                )
                .expectNextCount(bufferSize)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle backpressure in bulk operations")
    void bulkOperations_shouldHandleBackpressure() {
        int operationCount = 1000;

        Mono<List<Integer>> execution = Flux.range(0, operationCount)
                .flatMap(i -> createProductoStatus("backpressure-product-" + i), 10)
                .take(100)
                .collectList();

        StepVerifier.create(execution)
                .assertNext(statuses -> {
                    assertThat(statuses).hasSize(100);
                    assertThat(statuses).allMatch(this::is2xx);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should manage resources efficiently under load")
    void resourceManagement_shouldBeEfficient() {
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            Integer status = getProductosStatus().block(Duration.ofSeconds(5));
            assertThat(status).isNotNull();
            assertThat(is2xx(status)).isTrue();
        }

        Integer finalStatus = getProductosStatus().block(Duration.ofSeconds(5));
        assertThat(finalStatus).isNotNull();
        assertThat(is2xx(finalStatus)).isTrue();
    }

    @Test
    @DisplayName("Should handle race conditions gracefully")
    void raceConditions_shouldHandleGracefully() throws InterruptedException {
        String productId = createProductoAndReturnId("race-test-product")
                .block(Duration.ofSeconds(10));

        assertThat(productId).isNotBlank();

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger otherCount = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    Integer status = updateProductoStatus(
                            productId,
                            buildProductoRequest("race-update-" + index))
                            .block(Duration.ofSeconds(10));

                    if (status != null && status >= 200 && status < 300) {
                        successCount.incrementAndGet();
                    } else if (status != null && status == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        otherCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get() + conflictCount.get() + otherCount.get()).isEqualTo(10);
        assertThat(successCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void sustainedLoad_shouldMaintainPerformance() {
        int durationSeconds = 10;
        int requestsPerSecond = 20;
        int totalRequests = durationSeconds * requestsPerSecond;

        List<Integer> statuses = Flux.interval(Duration.ofMillis(1000L / requestsPerSecond))
                .take(totalRequests)
                .flatMap(tick -> getProductosStatus(), 10)
                .collectList()
                .block(Duration.ofSeconds(durationSeconds + 15L));

        assertThat(statuses).isNotNull();
        long successCount = statuses.stream().filter(this::is2xx).count();

        int expectedMinRequests = (int) (totalRequests * 0.8);
        assertThat(successCount).isGreaterThanOrEqualTo(expectedMinRequests);
    }

    private void seedProducts(int count, String prefix) {
        Flux.range(0, count)
                .concatMap(i -> createProductoAndReturnId(prefix + i))
                .blockLast(Duration.ofSeconds(60));
    }

    private Mono<Integer> getProductosStatus() {
        return webClient.get()
                .uri("/api/v2/productos")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> response.statusCode().value()))
                .onErrorReturn(599);
    }

    private Mono<Integer> createProductoStatus(String nombre) {
        return webClient.post()
                .uri("/api/v2/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildProductoRequest(nombre))
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> response.statusCode().value()))
                .onErrorReturn(599);
    }

    private Mono<String> createProductoAndReturnId(String nombre) {
        return webClient.post()
                .uri("/api/v2/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildProductoRequest(nombre))
                .exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.createException().flatMap(Mono::error);
                    }
                    return response.bodyToMono(JsonNode.class)
                            .map(this::extractId);
                });
    }

    private Mono<Integer> updateProductoStatus(String id, ProductoRequest request) {
        return webClient.put()
                .uri("/api/v2/productos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> response.statusCode().value()))
                .onErrorReturn(599);
    }

    private Mono<Boolean> consumeFirstStreamEvent() {
        return webClient.get()
                .uri("/api/v2/productos/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .take(1)
                .count()
                .map(count -> count == 1)
                .onErrorReturn(false);
    }

    private Mono<Long> timedReadRequest() {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return getProductosStatus()
                    .map(status -> {
                        assertThat(is2xx(status)).isTrue();
                        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    });
        });
    }

    private Mono<Long> timedCreateRequest(String nombre) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return createProductoStatus(nombre)
                    .map(status -> {
                        assertThat(is2xx(status)).isTrue();
                        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    });
        });
    }

    private ProductoRequest buildProductoRequest(String nombre) {
        return ProductoRequest.builder()
                .nombre(nombre)
                .descripcion("Test product: " + nombre)
                .precio(new BigDecimal("99.99"))
                .stock(10)
                .categoria("Test Category")
                .activo(true)
                .build();
    }

    private String extractId(JsonNode body) {
        if (body.hasNonNull("id")) {
            return body.get("id").asText();
        }
        if (body.hasNonNull("productoId")) {
            return body.get("productoId").asText();
        }
        if (body.hasNonNull("productId")) {
            return body.get("productId").asText();
        }
        throw new IllegalStateException("La respuesta del POST /api/v2/productos no contiene un campo id conocido");
    }

    private boolean is2xx(Integer status) {
        return status != null && status >= 200 && status < 300;
    }
}