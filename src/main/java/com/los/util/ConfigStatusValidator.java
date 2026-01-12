package com.los.util;

import com.los.config.ConfigStatus;
import com.los.dto.runtime.ValidationErrorResponse;
import com.los.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * Utility class for validating and setting default config status.
 */
@Slf4j
public class ConfigStatusValidator {

    private ConfigStatusValidator() {
        // Utility class
    }

    /**
     * Validates status and sets default if not provided or invalid.
     * 
     * @param status the status to validate
     * @return validated status or default DRAFT
     * @throws ValidationException if status is invalid
     */
    public static String validateAndSetDefault(String status) {
        // If status is null or blank, use default
        if (status == null || status.isBlank()) {
            log.debug("Status not provided, defaulting to DRAFT");
            return ConfigStatus.getDefault();
        }
        
        // Validate status value
        if (!ConfigStatus.isValid(status)) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_VALUE")
                    .message(String.format("Invalid status '%s'. Must be one of: DRAFT, ACTIVE, INACTIVE, DEPRECATED", status))
                    .build()
            ));
        }
        
        return status.toUpperCase();
    }
    
    /**
     * Validates status if provided, returns null if not provided.
     * For update operations where status is optional.
     * 
     * @param status the status to validate
     * @return validated status or null if not provided
     * @throws ValidationException if status is invalid
     */
    public static String validateIfProvided(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        
        if (!ConfigStatus.isValid(status)) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_VALUE")
                    .message(String.format("Invalid status '%s'. Must be one of: DRAFT, ACTIVE, INACTIVE, DEPRECATED", status))
                    .build()
            ));
        }
        
        return status.toUpperCase();
    }
}
