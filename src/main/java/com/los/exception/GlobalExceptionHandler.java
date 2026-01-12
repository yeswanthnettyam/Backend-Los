package com.los.exception;

import com.los.dto.runtime.ErrorResponse;
import com.los.dto.runtime.ValidationErrorResponse;
import com.los.util.CorrelationIdHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for all REST controllers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        String correlationId = CorrelationIdHolder.get();
        log.warn("[{}] Validation error: {}", correlationId, ex.getMessage());
        
        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .errors(ex.getErrors())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String correlationId = CorrelationIdHolder.get();
        log.warn("[{}] Request validation error", correlationId);
        
        List<ValidationErrorResponse.FieldError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(ValidationErrorResponse.FieldError.builder()
                    .fieldId(error.getField())
                    .code("INVALID_REQUEST")
                    .message(error.getDefaultMessage())
                    .build());
        });
        
        ValidationErrorResponse response = ValidationErrorResponse.builder()
                .errors(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConfigNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConfigNotFound(ConfigNotFoundException ex) {
        String correlationId = CorrelationIdHolder.get();
        log.error("[{}] Configuration not found: {}", correlationId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("CONFIG_NOT_FOUND")
                .message(ex.getMessage())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String correlationId = CorrelationIdHolder.get();
        log.error("[{}] Internal server error", correlationId, ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

