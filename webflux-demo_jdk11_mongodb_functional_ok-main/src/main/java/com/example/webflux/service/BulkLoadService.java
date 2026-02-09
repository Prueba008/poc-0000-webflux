package com.example.webflux.service;


import com.example.webflux.model.ProductDoc;
import com.example.webflux.model.dto.bulk.BulkLoadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public abstract class BulkLoadService {


    public abstract Mono<BulkLoadResult> loadProducts(Flux<ProductDoc> input);
}
