package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates field value against a regex pattern.
 */
@Component
public class RegexValidationRule implements ValidationRule {

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        return fieldRules.containsKey("pattern");
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, Map<String, Object> allFormData) {
        if (fieldValue == null) {
            return ValidationResult.success(); // Skip if null
        }

        String pattern = (String) fieldRules.get("pattern");
        String errorMessage = (String) fieldRules.getOrDefault("patternMessage", "Invalid format");

        if (!Pattern.matches(pattern, fieldValue.toString())) {
            return ValidationResult.failure("INVALID_FORMAT", errorMessage);
        }

        return ValidationResult.success();
    }
}

