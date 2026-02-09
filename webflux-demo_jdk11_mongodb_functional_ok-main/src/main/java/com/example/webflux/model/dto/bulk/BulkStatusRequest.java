package com.example.webflux.model.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class BulkStatusRequest {
    private List<String> ids;
    private Boolean activo;
}