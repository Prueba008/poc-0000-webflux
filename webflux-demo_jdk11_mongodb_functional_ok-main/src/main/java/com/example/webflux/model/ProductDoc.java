package com.example.webflux.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;


// Modelo Mongo (ejemplo “Product”)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "products")
public class ProductDoc {
  @Id
  private String id;

  @Indexed
  private String sku;

  private String name;
  private BigDecimal price;
  private Long ts;
}
