package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates PAN (Permanent Account Number) format.
 * Format: 5 letters + 4 digits + 1 letter (e.g., ABCDE1234F)
 */
@Component
public class PanValidationRule implements ValidationRule {

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        String dataType = (String) fieldRules.get("dataType");
        String fieldType = (String) fieldRules.get("type");
        return "PAN".equalsIgnoreCase(dataType) || "PAN".equalsIgnoreCase(fieldType) ||
               (fieldRules.containsKey("pattern") && 
                fieldRules.get("pattern").toString().contains("PAN"));
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, 
                                    Map<String, Object> allFormData) {
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            // Required check is handled by RequiredValidationRule
            return ValidationResult.success();
        }

        String pan = fieldValue.toString().trim().toUpperCase();

        // Validate PAN format
        if (!PAN_PATTERN.matcher(pan).matches()) {
            String errorMessage = (String) fieldRules.getOrDefault("patternMessage", 
                    "Invalid PAN format. Must be 5 letters, 4 digits, 1 letter (e.g., ABCDE1234F)");
            return ValidationResult.failure("INVALID_PAN_FORMAT", errorMessage);
        }

        return ValidationResult.success();
    }
}
