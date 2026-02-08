package com.example.webflux.handler;

import com.example.webflux.exception.ApiException;
import com.example.webflux.model.dto.ErrorResponse;
import com.example.webflux.observability.CorrelationIdWebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    @Value("${app.errors.includeStacktrace:false}")
    private boolean includeStacktrace;

    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
        this.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable ex = getError(request);

        HttpStatus status = mapStatus(ex);
        String code = mapCode(ex, status);
        String message = mapMessage(ex, status);

        Map<String, Object> attrs = getErrorAttributes(request,
                includeStacktrace ?
                        ErrorAttributeOptions.of(
                                ErrorAttributeOptions.Include.MESSAGE,
                                ErrorAttributeOptions.Include.BINDING_ERRORS,
                                ErrorAttributeOptions.Include.EXCEPTION,
                                ErrorAttributeOptions.Include.STACK_TRACE
                        )
                        : ErrorAttributeOptions.of(
                                ErrorAttributeOptions.Include.MESSAGE,
                                ErrorAttributeOptions.Include.BINDING_ERRORS
                        )
        );

        String path = String.valueOf(attrs.getOrDefault("path", request.path()));
        String error = status.getReasonPhrase();

        String correlationId = request.exchange().getAttributeOrDefault(CorrelationIdWebFilter.CTX_KEY, null);
        if (correlationId == null) {
            correlationId = request.exchange().getResponse().getHeaders().getFirst("X-Correlation-Id");
        }

        List<ErrorResponse.FieldError> fieldErrors = mapFieldErrors(attrs);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(System.currentTimeMillis())
                .status(status.value())
                .error(error)
                .code(code)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();

        log.error("Error {} {} -> {} {} (code={}): {}", request.methodName(), path, status.value(), error, code, message, ex);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }

    private HttpStatus mapStatus(Throwable ex) {
        if (ex instanceof ApiException) {
            return ((ApiException) ex).getStatus();
        }
        if (ex instanceof ServerWebInputException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String mapCode(Throwable ex, HttpStatus status) {
        if (ex instanceof ApiException) {
            return ((ApiException) ex).getCode();
        }
        if (status == HttpStatus.BAD_REQUEST) return "BAD_REQUEST";
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) return "INTERNAL_ERROR";
        return status.name();
    }

    private String mapMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof ApiException) return ex.getMessage();
        if (ex instanceof ServerWebInputException) return ex.getMessage() != null ? ex.getMessage() : "Solicitud inválida";
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) return "Error interno del servidor";
        return ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
    }

    @SuppressWarnings("unchecked")
    private List<ErrorResponse.FieldError> mapFieldErrors(Map<String, Object> attrs) {
        Object errors = attrs.get("errors");
        if (!(errors instanceof List)) return Collections.emptyList();

        List<Map<String, Object>> list = (List<Map<String, Object>>) errors;
        return list.stream()
                .map(e -> ErrorResponse.FieldError.builder()
                        .field(String.valueOf(e.getOrDefault("field", "")))
                        .message(String.valueOf(e.getOrDefault("defaultMessage", e.getOrDefault("message", "inválido"))))
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
