package com.example.webflux.config;

import com.example.webflux.model.Producto;
import com.example.webflux.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Profile({"dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository repo;

    @Override
    public void run(String... args) {
        List<Producto> seed = List.of(
                Producto.builder().nombre("Laptop Dell XPS 13").descripcion("Laptop ultradelgada").precio(new BigDecimal("1299.99")).stock(15).categoria("Computación").activo(true).fechaCreacion(Instant.now()).fechaActualizacion(Instant.now()).build(),
                Producto.builder().nombre("iPhone 14 Pro").descripcion("Smartphone Apple").precio(new BigDecimal("999.00")).stock(25).categoria("Electrónica").activo(true).fechaCreacion(Instant.now()).fechaActualizacion(Instant.now()).build(),
                Producto.builder().nombre("Logitech MX Master 3").descripcion("Mouse inalámbrico").precio(new BigDecimal("99.99")).stock(50).categoria("Computación").activo(true).fechaCreacion(Instant.now()).fechaActualizacion(Instant.now()).build()
        );

        repo.count()
                .flatMapMany(cnt -> cnt == 0 ? repo.saveAll(Flux.fromIterable(seed)) : Flux.empty())
                .doOnNext(p -> log.info("Seed producto: {}", p.getNombre()))
                .doOnError(e -> log.error("Error seed", e))
                .subscribe();
    }
}
