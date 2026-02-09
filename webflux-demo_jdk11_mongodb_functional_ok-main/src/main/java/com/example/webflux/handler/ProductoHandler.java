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
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoHandler {

    private final ProductService service;
    private final Validator validator;

    public Mono<ServerResponse> getAll(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAllActivos(), ProductoResponse.class);
    }

    public Mono<ServerResponse> getById(ServerRequest req) {
        String id = req.pathVariable("id");
        return service.findById(id)
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(ProductoRequest.class)
                .doOnNext(r -> ValidationSupport.validateOrThrow(validator, r))
                .flatMap(service::create)
                .flatMap(created -> ServerResponse.created(URI.create("/api/v2/productos/" + created.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(created));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        String id = req.pathVariable("id");
        return req.bodyToMono(ProductoRequest.class)
                .doOnNext(r -> ValidationSupport.validateOrThrow(validator, r))
                .flatMap(r -> service.update(id, r))
                .flatMap(p -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(p));
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        String id = req.pathVariable("id");
        return service.softDelete(id)
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> stream(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.findAllActivos().delayElements(Duration.ofSeconds(1)),
                        ProductoResponse.class);
    }
}
