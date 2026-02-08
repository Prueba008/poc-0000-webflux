package com.example.webflux.model.dto;

import com.example.webflux.model.Producto;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {

    private String id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private String categoria;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
    private Boolean activo;
    private Boolean disponible;

    public static ProductoResponse fromEntity(Producto p) {
        return ProductoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .categoria(p.getCategoria())
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .activo(p.getActivo())
                .disponible(p.isDisponible())
                .build();
    }
}
