package com.example.webflux.config;

import com.example.webflux.model.Producto;
import com.example.webflux.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Componente para la precarga de datos iniciales en el entorno de desarrollo.
 * Implementa CommandLineRunner para ejecutarse tras el inicio del contexto de Spring.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository repo;

    @Override
    public void run(String... args) {
        log.info("Verificando integridad de datos en el perfil DEV...");

        // Verificamos si existen datos para evitar duplicidad en reinicios
        repo.count()
            .filter(count -> count == 0)
            .flatMapMany(count -> repo.saveAll(getSeedData()))
            .doOnNext(p -> log.debug("Producto sembrado: {} (ID: {})", p.getNombre(), p.getId()))
            .then() // Espera a que termine el Flux
            .doOnSuccess(v -> log.info("Proceso de inicialización de datos completado con éxito"))
            .doOnError(e -> log.error("Fallo crítico en la siembra de datos: {}", e.getMessage()))
            /* En CommandLineRunner, bloqueamos intencionalmente para asegurar que 
               la inicialización termine antes de que el servidor acepte peticiones.
            */
            .block(); 
    }

    /**
     * Genera los datos semilla con una estampa de tiempo unificada.
     */
    private Flux<Producto> getSeedData() {
        Instant now = Instant.now();
        return Flux.just(
            createProducto("Laptop Dell XPS 13", "Laptop ultradelgada", "1299.99", 15, "Computación", now),
            createProducto("iPhone 14 Pro", "Smartphone Apple", "999.00", 25, "Electrónica", now),
            createProducto("Logitech MX Master 3", "Mouse inalámbrico", "99.99", 50, "Computación", now)
        );
    }

    private Producto createProducto(String nom, String desc, String precio, int stock, String cat, Instant now) {
        return Producto.builder()
                .nombre(nom)
                .descripcion(desc)
                .precio(new BigDecimal(precio))
                .stock(stock)
                .categoria(cat)
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
    }
}