package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates min/max constraints for numbers and string length.
 */
@Component
public class MinMaxValidationRule implements ValidationRule {

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        return fieldRules.containsKey("min") || fieldRules.containsKey("max");
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, Map<String, Object> allFormData) {
        if (fieldValue == null) {
            return ValidationResult.success(); // Skip if null (handled by required rule)
        }

        String dataType = (String) fieldRules.getOrDefault("dataType", "STRING");
        
        if ("NUMBER".equals(dataType)) {
            return validateNumber(fieldValue, fieldRules);
        } else {
            return validateStringLength(fieldValue, fieldRules);
        }
    }

    private ValidationResult validateNumber(Object fieldValue, Map<String, Object> fieldRules) {
        try {
            double value = Double.parseDouble(fieldValue.toString());
            
            if (fieldRules.containsKey("min")) {
                double min = ((Number) fieldRules.get("min")).doubleValue();
                if (value < min) {
                    return ValidationResult.failure("MIN_VALUE", "Value must be at least " + min);
                }
            }
            
            if (fieldRules.containsKey("max")) {
                double max = ((Number) fieldRules.get("max")).doubleValue();
                if (value > max) {
                    return ValidationResult.failure("MAX_VALUE", "Value must be at most " + max);
                }
            }
            
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("INVALID_NUMBER", "Invalid number format");
        }
    }

    private ValidationResult validateStringLength(Object fieldValue, Map<String, Object> fieldRules) {
        int length = fieldValue.toString().length();
        
        if (fieldRules.containsKey("minLength")) {
            int min = ((Number) fieldRules.get("minLength")).intValue();
            if (length < min) {
                return ValidationResult.failure("MIN_LENGTH", "Minimum length is " + min);
            }
        }
        
        if (fieldRules.containsKey("maxLength")) {
            int max = ((Number) fieldRules.get("maxLength")).intValue();
            if (length > max) {
                return ValidationResult.failure("MAX_LENGTH", "Maximum length is " + max);
            }
        }
        
        return ValidationResult.success();
    }
}

