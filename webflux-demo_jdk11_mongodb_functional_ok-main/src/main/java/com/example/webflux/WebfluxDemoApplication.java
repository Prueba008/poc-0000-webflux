package com.example.webflux;

import com.example.webflux.observability.ReactorMdc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;

@SpringBootApplication
public class WebfluxDemoApplication {

    @PostConstruct
    public void init() {
        // Propaga correlationId (MDC) a través de operadores Reactor
        ReactorMdc.enable();
    }

    public static void main(String[] args) {
        SpringApplication.run(WebfluxDemoApplication.class, args);
    }
}
