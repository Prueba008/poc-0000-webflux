package com.example.webflux.model.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateRequest {
    private List<ProductoCreate> productos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoCreate {
        private String nombre;
        private String descripcion;
        private BigDecimal precio;
        private Integer stock;
        private String categoria;
        private Boolean activo;
    }
}