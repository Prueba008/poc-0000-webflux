package com.example.webflux.handler;

import com.example.webflux.model.dto.producto.ProductoRequest;
import com.example.webflux.model.dto.producto.ProductoResponse;
import com.example.webflux.service.ProductService;
import com.example.webflux.validation.ValidationSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.Validator;
import java.net.URI;

/**
 * Handler funcional para la gestión de productos.
 * Encapsula la lógica de procesamiento de solicitudes, validación y transformación
 * de respuestas, manteniendo el desacoplamiento de las rutas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoHandler {

    private final ProductService service;
    private final Validator validator;

    /**
     * Procesa la creación de un nuevo producto.
     * Realiza validación JSR-303 proactiva antes de invocar la capa de servicio.
     */
    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(ProductoRequest.class)
                .doOnNext(body -> ValidationSupport.validateOrThrow(validator, body))
                .flatMap(service::create)
                .flatMap(res -> ServerResponse.created(URI.create(req.path() + "/" + res.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(res))
                .doOnSuccess(r -> log.info("Producto creado exitosamente"));
    }

    /**
     * Actualiza un producto existente de forma total.
     */
    public Mono<ServerResponse> update(ServerRequest req) {
        String id = req.pathVariable("id");
        return req.bodyToMono(ProductoRequest.class)
                .doOnNext(body -> ValidationSupport.validateOrThrow(validator, body))
                .flatMap(body -> service.update(id, body))
                .flatMap(res -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(res))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Recupera un producto por su ID. 
     * Implementa switchIfEmpty para retornar 404 Not Found si el Mono resultante es vacío.
     */
    public Mono<ServerResponse> getById(ServerRequest req) {
        String id = req.pathVariable("id");
        return service.findById(id)
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Retorna todos los productos activos.
     * Utiliza el body de ServerResponse para propagar el Flux, permitiendo el manejo de Backpressure.
     */
    public Mono<ServerResponse> getAll(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAllActivos(), ProductoResponse.class);
    }

    /**
     * Genera un flujo SSE (Server-Sent Events) para actualizaciones en tiempo real.
     */
    public Mono<ServerResponse> stream(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.findDisponibles(), ProductoResponse.class);
    }

    /**
     * Ejecuta el borrado lógico de un producto.
     */
    public Mono<ServerResponse> delete(ServerRequest req) {
        return service.softDelete(req.pathVariable("id"))
                .then(ServerResponse.noContent().build());
    }
}