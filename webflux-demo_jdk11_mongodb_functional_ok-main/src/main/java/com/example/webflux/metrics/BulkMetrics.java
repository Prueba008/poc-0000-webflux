package com.example.webflux.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BulkMetrics {

  private final Counter bulkItems;
  private final Counter bulkErrors;
  private final Timer bulkTimer;
  private final AtomicInteger inFlight = new AtomicInteger(0);

  public BulkMetrics(MeterRegistry registry) {
    this.bulkItems = Counter.builder("bulk_items_total")
        .description("Total items recibidos en operaciones bulk")
        .register(registry);

    this.bulkErrors = Counter.builder("bulk_errors_total")
        .description("Total errores en operaciones bulk")
        .register(registry);

    this.bulkTimer = Timer.builder("bulk_operation_seconds")
        .description("Duración de la operación bulk")
        .publishPercentileHistogram()
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .register(registry);

    Gauge.builder("bulk_in_flight", inFlight, AtomicInteger::get)
        .description("Cantidad de operaciones bulk en vuelo")
        .register(registry);
  }

  public <T> Mono<T> wrapBulk(String operation, long expectedItems, Mono<T> mono) {
    inFlight.incrementAndGet();

    return mono
        .doOnSubscribe(s -> bulkItems.increment(expectedItems))
        .doOnError(e -> bulkErrors.increment())
        .transform(m -> Mono.defer(() -> {
          Timer.Sample sample = Timer.start();
          return m.doFinally(sig -> {
            sample.stop(bulkTimer);
            inFlight.decrementAndGet();
          });
        }));
  }
}
