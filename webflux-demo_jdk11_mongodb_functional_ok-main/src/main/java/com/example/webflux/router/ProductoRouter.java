package com.example.webflux.router;

import com.example.webflux.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Definición de rutas funcionales para la API de Productos v2.
 * Utiliza {@link RouterFunction} para un enrutamiento más eficiente y ligero que @RestController.
 */
@Configuration
public class ProductoRouter {

    @Bean
    public RouterFunction<ServerResponse> productoRoutes(ProductoHandler h) {
        return nest(path("/api/v2/productos"),
            route(GET("").and(accept(MediaType.APPLICATION_JSON)), h::getAll)
                .andRoute(GET("/{id}").and(accept(MediaType.APPLICATION_JSON)), h::getById)
                .andRoute(POST("").and(contentType(MediaType.APPLICATION_JSON)), h::create)
                .andRoute(PUT("/{id}").and(contentType(MediaType.APPLICATION_JSON)), h::update)
                .andRoute(DELETE("/{id}"), h::delete)
                .andRoute(GET("/stream").and(accept(MediaType.TEXT_EVENT_STREAM)), h::stream)
        );
    }
}