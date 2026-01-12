package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates that a required field has a value.
 */
@Component
public class RequiredValidationRule implements ValidationRule {

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        return Boolean.TRUE.equals(fieldRules.get("required"));
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, Map<String, Object> allFormData) {
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            return ValidationResult.failure("REQUIRED", "This field is required");
        }
        return ValidationResult.success();
    }
}

