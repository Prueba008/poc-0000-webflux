package com.example.webflux.model.dto.bulk;

import com.example.webflux.model.dto.ErrorDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResult {
    private String operation;
    private int successCount;
    private int failedCount;
    private List<String> successIds;
    private List<ErrorDetail> errors;
}