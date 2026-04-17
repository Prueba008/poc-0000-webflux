package com.example.webflux.model.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para actualización parcial de productos en operaciones bulk.
 * Contiene solo los campos que se desean actualizar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoBulkUpdateItem {
    
    private String id;
    
    private String nombre;
    
    private String descripcion;
    
    private BigDecimal precio;
    
    private Integer stock;
    
    private String categoria;
    
    private Boolean activo;
}
