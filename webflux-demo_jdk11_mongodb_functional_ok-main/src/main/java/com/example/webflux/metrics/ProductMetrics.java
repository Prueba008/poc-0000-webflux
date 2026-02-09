package com.example.webflux.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ProductMetrics {

    private final Counter bulkCreateCounter;
    private final Counter bulkUpdateCounter;
    private final Counter bulkDeleteCounter;
    private final Counter bulkStatusUpdateCounter;
    private final Counter bulkStockUpdateCounter;

    public ProductMetrics(MeterRegistry registry) {
        bulkCreateCounter = Counter.builder("producto.bulk.operations")
                .tag("operation", "create")
                .description("Total de operaciones bulk create")
                .register(registry);

        bulkUpdateCounter = Counter.builder("producto.bulk.operations")
                .tag("operation", "update")
                .description("Total de operaciones bulk update")
                .register(registry);

        bulkDeleteCounter = Counter.builder("producto.bulk.operations")
                .tag("operation", "delete")
                .description("Total de operaciones bulk delete")
                .register(registry);

        bulkStatusUpdateCounter = Counter.builder("producto.bulk.operations")
                .tag("operation", "status_update")
                .description("Total de operaciones bulk status update")
                .register(registry);

        bulkStockUpdateCounter = Counter.builder("producto.bulk.operations")
                .tag("operation", "stock_update")
                .description("Total de operaciones bulk stock update")
                .register(registry);
    }

    public void incrementBulkCreateCounter() {
        bulkCreateCounter.increment();
    }

    public void incrementBulkUpdateCounter() {
        bulkUpdateCounter.increment();
    }

    public void incrementBulkDeleteCounter() {
        bulkDeleteCounter.increment();
    }

    public void incrementBulkStatusUpdateCounter() {
        bulkStatusUpdateCounter.increment();
    }

    public void incrementBulkStockUpdateCounter() {
        bulkStockUpdateCounter.increment();
    }
}
