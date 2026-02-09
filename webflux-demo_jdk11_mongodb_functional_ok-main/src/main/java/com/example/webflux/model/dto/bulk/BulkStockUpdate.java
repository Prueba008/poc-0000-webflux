package com.example.webflux.model.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkStockUpdate {
    private List<StockUpdateItem> updates;
    private String operacion; // "SET", "INCREMENT", "DECREMENT"

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateItem {
        private String productoId;
        private Integer cantidad;
    }
}