package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates Aadhaar number format.
 * Supports both masked (XXXX XXXX 1234) and full (1234 5678 9012) formats.
 * Format: 12 digits, optionally with spaces (XXXX XXXX XXXX)
 */
@Component
public class AadhaarValidationRule implements ValidationRule {

    // Pattern for full Aadhaar: 12 digits, optionally with spaces
    private static final Pattern FULL_AADHAAR_PATTERN = Pattern.compile("^[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{4}$");
    
    // Pattern for masked Aadhaar: XXXX XXXX 1234 (last 4 digits visible)
    private static final Pattern MASKED_AADHAAR_PATTERN = Pattern.compile("^[Xx]{4}\\s?[Xx]{4}\\s?[0-9]{4}$");

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        String dataType = (String) fieldRules.get("dataType");
        String fieldType = (String) fieldRules.get("type");
        return "AADHAAR".equalsIgnoreCase(dataType) || "AADHAAR".equalsIgnoreCase(fieldType) ||
               (fieldRules.containsKey("pattern") && 
                fieldRules.get("pattern").toString().contains("AADHAAR"));
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, 
                                    Map<String, Object> allFormData) {
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            // Required check is handled by RequiredValidationRule
            return ValidationResult.success();
        }

        String aadhaar = fieldValue.toString().trim();

        // Check if masked format is allowed
        boolean allowMasked = Boolean.TRUE.equals(fieldRules.get("allowMasked"));
        
        // Validate format
        boolean isValidFull = FULL_AADHAAR_PATTERN.matcher(aadhaar).matches();
        boolean isValidMasked = allowMasked && MASKED_AADHAAR_PATTERN.matcher(aadhaar).matches();

        if (!isValidFull && !isValidMasked) {
            String errorMessage;
            if (allowMasked) {
                errorMessage = (String) fieldRules.getOrDefault("patternMessage", 
                        "Invalid Aadhaar format. Must be 12 digits (e.g., 1234 5678 9012) or masked (e.g., XXXX XXXX 1234)");
            } else {
                errorMessage = (String) fieldRules.getOrDefault("patternMessage", 
                        "Invalid Aadhaar format. Must be 12 digits (e.g., 1234 5678 9012)");
            }
            return ValidationResult.failure("INVALID_AADHAAR_FORMAT", errorMessage);
        }

        // Additional validation: Check if Aadhaar doesn't start with 0 or 1 (invalid Aadhaar numbers)
        if (isValidFull) {
            String digitsOnly = aadhaar.replaceAll("\\s", "");
            if (digitsOnly.startsWith("0") || digitsOnly.startsWith("1")) {
                String errorMessage = (String) fieldRules.getOrDefault("patternMessage", 
                        "Invalid Aadhaar number. Cannot start with 0 or 1");
                return ValidationResult.failure("INVALID_AADHAAR_NUMBER", errorMessage);
            }
        }

        return ValidationResult.success();
    }
}
