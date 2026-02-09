package com.example.webflux.model.dto.bulk;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BulkLoadResult {
  private long received;
  private long inserted;
  private long failed;
  private long ms;
}
