package com.example.webflux.router;

import com.example.webflux.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class ProductoRouter {

    @Bean
    public RouterFunction<ServerResponse> productoRoutes(ProductoHandler h) {
        return route(GET("/api/v2/productos").and(accept(MediaType.APPLICATION_JSON)), h::getAll)
                .andRoute(GET("/api/v2/productos/{id}").and(accept(MediaType.APPLICATION_JSON)), h::getById)
                .andRoute(POST("/api/v2/productos").and(contentType(MediaType.APPLICATION_JSON)), h::create)
                .andRoute(PUT("/api/v2/productos/{id}").and(contentType(MediaType.APPLICATION_JSON)), h::update)
                .andRoute(DELETE("/api/v2/productos/{id}"), h::delete)
                .andRoute(GET("/api/v2/productos/stream").and(accept(MediaType.TEXT_EVENT_STREAM)), h::stream);
    }
}
