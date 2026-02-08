package com.example.webflux.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "productos")
public class Producto {

    @Id
    private String id;

    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private String categoria;

    private Boolean activo;

    private Instant fechaCreacion;
    private Instant fechaActualizacion;

    public boolean isDisponible() {
        return Boolean.TRUE.equals(activo) && stock != null && stock > 0;
    }
}
