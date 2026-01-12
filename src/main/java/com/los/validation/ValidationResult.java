package com.los.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Result of a validation check.
 */
@Data
@AllArgsConstructor
@Builder
public class ValidationResult {

    private boolean valid;
    private String errorCode;
    private String errorMessage;

    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    public static ValidationResult failure(String errorCode, String errorMessage) {
        return ValidationResult.builder()
                .valid(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}

