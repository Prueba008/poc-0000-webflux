package com.example.webflux.exception;

import org.springframework.http.HttpStatus;

/**
 * Registro centralizado de excepciones de negocio para el ecosistema WebFlux.
 * * Esta clase agrupa las especializaciones de {@link ApiException} para evitar la dispersión
 * de archivos y estandarizar los códigos de error internos del sistema.
 * * @see ApiException
 */
public abstract class BusinessException extends ApiException {

    private BusinessException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    /**
     * Lanzada cuando el recurso solicitado no existe en la base de datos.
     * Uso común: .switchIfEmpty(Mono.error(new BusinessException.NotFound("Producto no encontrado")))
     */
    public static class NotFound extends ApiException {
        public NotFound(String message) {
            super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
        }
    }

    /**
     * Indica que la solicitud infringe reglas de validación de negocio o lógica inconsistente.
     * Ejemplo: Intentar reducir stock por debajo de cero.
     */
    public static class BadRequest extends ApiException {
        public BadRequest(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_BUSINESS_REQUEST", message);
        }
    }

    /**
     * Indica un conflicto con el estado actual del servidor.
     * Uso común: Violación de SKU duplicado o nombres de productos únicos.
     */
    public static class Conflict extends ApiException {
        public Conflict(String message) {
            super(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS", message);
        }
    }

    /**
     * Excepción para fallos en operaciones masivas (Bulk Operations).
     */
    public static class BulkOperationException extends ApiException {
        public BulkOperationException(String message) {
            super(HttpStatus.MULTI_STATUS, "BULK_PARTIAL_FAILURE", message);
        }
    }
}