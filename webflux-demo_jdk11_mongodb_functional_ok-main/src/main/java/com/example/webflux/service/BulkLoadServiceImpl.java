package com.example.webflux.service;

import com.example.webflux.metrics.ProductMetrics;
import com.example.webflux.model.ProductDoc;
import com.example.webflux.model.Producto;
import com.example.webflux.model.dto.bulk.BulkLoadResult;
import com.example.webflux.repository.ProductoRepository;
import com.example.webflux.service.BulkLoadService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
public class BulkLoadServiceImpl extends BulkLoadService {

    private final ReactiveMongoTemplate mongo;
    private final Counter globalInsertedCounter;

    private static final int BATCH_SIZE = 2000;
    private static final int MAX_IN_FLIGHT_BATCHES = 2;
    private final ProductoRepository repository;
    private final ProductMetrics metrics;

    /**
     * Procesa la carga de 1M de registros desde un flujo de datos.
     * Utiliza Backpressure para no saturar la base de datos.
     */
    public Mono<Void> processBulkCsv(Flux<Producto> csvFlux) {
        final int BATCH_SIZE = 1000;
        final int CONCURRENCY = 8;

        return csvFlux
                .buffer(BATCH_SIZE) // Agrupa registros para reducir operaciones de red
                .flatMap(batch -> repository.saveAll(batch)
                                .doOnComplete(() -> metrics.recordOperation("bulk_load", batch.size())),
                        CONCURRENCY) // Controla el paralelismo de escritura
                .then()
                .doOnSuccess(v -> log.info("Carga masiva de 1M finalizada exitosamente"))
                .doOnError(e -> log.error("Error en la carga masiva: {}", e.getMessage()));
    }
    public BulkLoadServiceImpl(ReactiveMongoTemplate mongo, MeterRegistry registry, ProductoRepository repository, ProductMetrics metrics) {
        this.mongo = mongo;
        // Registro de métrica vía Micrometer
        this.globalInsertedCounter = Counter.builder("products.bulk.inserted")
                .description("Total de productos insertados vía BulkLoad")
                .tag("service", "bulk-loader")
                .register(registry);
        this.repository = repository;
        this.metrics = metrics;
    }

    @Override
    public Mono<BulkLoadResult> loadProducts(Flux<ProductDoc> input) {
        long start = System.currentTimeMillis();

        // LongAdder optimiza el rendimiento en JDK 11 bajo alta contención
        final LongAdder received = new LongAdder();
        final LongAdder inserted = new LongAdder();
        final LongAdder failed = new LongAdder();

        return input
                .doOnNext(p -> received.increment())
                .bufferTimeout(BATCH_SIZE, Duration.ofSeconds(2)) // Manejo de Backpressure
                .filter(batch -> !batch.isEmpty())
                .flatMap(batch -> mongo.insertAll(batch)
                                .count()
                                .doOnNext(count -> {
                                    inserted.add(count);
                                    globalInsertedCounter.increment(count);
                                })
                                .onErrorResume(ex -> {
                                    log.error("Fallo en lote de inserción masiva: {}", ex.getMessage());
                                    failed.add(batch.size());
                                    return Mono.just(0L);
                                }),
                        MAX_IN_FLIGHT_BATCHES // Límite de concurrencia en escritura
                )
                .then(Mono.fromSupplier(() -> BulkLoadResult.builder()
                        .received(received.sum())
                        .inserted(inserted.sum())
                        .failed(failed.sum())
                        .ms(System.currentTimeMillis() - start)
                        .build()));
    }

}