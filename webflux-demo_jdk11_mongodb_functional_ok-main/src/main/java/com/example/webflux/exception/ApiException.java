package com.example.webflux.exception;

import org.springframework.http.HttpStatus;

/**
 * Clase base para excepciones de negocio en el ecosistema WebFlux.
 * Extiende de {@link RuntimeException} para integrarse con el flujo reactivo
 * sin obligar a declaraciones de firma 'throws'.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    /**
     * @param status Estado HTTP semántico (ej. 404, 400, 409).
     * @param code Código de error interno para el frontend (ej. PRODUCT_NOT_FOUND).
     * @param message Mensaje descriptivo para logs y respuesta.
     */
    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}