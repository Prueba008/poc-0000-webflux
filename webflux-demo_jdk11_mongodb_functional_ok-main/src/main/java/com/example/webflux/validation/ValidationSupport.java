package com.example.webflux.validation;

import com.example.webflux.exception.BadRequestException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

public final class ValidationSupport {

    private ValidationSupport() {}

    public static <T> void validateOrThrow(Validator validator, T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (violations == null || violations.isEmpty()) return;

        String msg = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        throw new BadRequestException("Error de validación: " + msg);
    }
}
