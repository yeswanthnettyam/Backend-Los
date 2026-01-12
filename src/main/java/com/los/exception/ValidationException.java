package com.los.exception;

import com.los.dto.runtime.ValidationErrorResponse;
import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when validation fails.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<ValidationErrorResponse.FieldError> errors;

    public ValidationException(List<ValidationErrorResponse.FieldError> errors) {
        super("Validation failed");
        this.errors = errors;
    }
}

