package com.example.webflux.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestión centralizada de métricas para el dominio de Productos.
 * Unifica el seguimiento de contadores de operación, métricas de volumen (bulk)
 * y monitoreo de saturación (in-flight).
 */
@Component
public class ProductMetrics {

    private final MeterRegistry registry;
    private final Timer bulkTimer;
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public ProductMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Timer para medir latencia y percentiles de operaciones masivas
        this.bulkTimer = Timer.builder("producto.bulk.duration")
                .description("Duración de las operaciones masivas de productos")
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);

        // Gauge para monitorear la saturación del sistema en tiempo real
        Gauge.builder("producto.bulk.inflight", inFlight, AtomicInteger::get)
                .description("Cantidad de operaciones masivas actualmente en ejecución")
                .register(registry);
    }

    /**
     * Registra un evento de operación con etiquetas dinámicas.
     * @param operation Tipo de operación (create, update, delete, stock_update)
     * @param count Cantidad de items procesados
     */
    public void recordOperation(String operation, double count) {
        Counter.builder("producto.operations.total")
                .tag("operation", operation)
                .description("Total de productos procesados por tipo de operación")
                .register(registry)
                .increment(count);
    }

    /**
     * Wrapper reactivo que automatiza la recolección de métricas durante el flujo.
     * Captura: Items totales, errores, latencia y operaciones en vuelo.
     * * @param operation Identificador de la operación para los tags
     * @param expectedItems Cantidad de items involucrados en el lote
     * @param source Flujo Mono original a monitorizar
     * @return Mono original enriquecido con señales de telemetría
     */
    public <T> Mono<T> monitorBulk(String operation, long expectedItems, Mono<T> source) {
        return Mono.defer(() -> {
            // Inicio de medición de saturación y tiempo
            inFlight.incrementAndGet();
            Timer.Sample sample = Timer.start(registry);
            
            return source
                .doOnSubscribe(s -> recordOperation(operation, expectedItems))
                .doOnError(e -> recordError(operation))
                .doFinally(signalType -> {
                    // Cierre de medición independientemente del resultado (éxito/error)
                    sample.stop(bulkTimer);
                    inFlight.decrementAndGet();
                });
        });
    }

    private void recordError(String operation) {
        Counter.builder("producto.operations.errors")
                .tag("operation", operation)
                .description("Total de errores detectados en operaciones de productos")
                .register(registry)
                .increment();
    }
}