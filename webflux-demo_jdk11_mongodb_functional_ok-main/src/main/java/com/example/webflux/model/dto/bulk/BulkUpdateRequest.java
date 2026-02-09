package com.example.webflux.model.dto.bulk;

import com.example.webflux.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRequest {
    private List<Producto> productos;
}
