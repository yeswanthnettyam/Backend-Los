package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates GST (Goods and Services Tax) number format.
 * Format: 15 characters - 2 state code + 10 PAN + 3 entity code + 1 check digit + 1 'Z' (e.g., 27ABCDE1234F1Z5)
 */
@Component
public class GstValidationRule implements ValidationRule {

    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        String dataType = (String) fieldRules.get("dataType");
        String fieldType = (String) fieldRules.get("type");
        return "GST".equalsIgnoreCase(dataType) || "GST".equalsIgnoreCase(fieldType) ||
               (fieldRules.containsKey("pattern") && 
                fieldRules.get("pattern").toString().contains("GST"));
    }

    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, 
                                    Map<String, Object> allFormData) {
        if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
            // Required check is handled by RequiredValidationRule
            return ValidationResult.success();
        }

        String gst = fieldValue.toString().trim().toUpperCase();

        // Validate GST format
        if (!GST_PATTERN.matcher(gst).matches()) {
            String errorMessage = (String) fieldRules.getOrDefault("patternMessage", 
                    "Invalid GST format. Must be 15 characters (e.g., 27ABCDE1234F1Z5)");
            return ValidationResult.failure("INVALID_GST_FORMAT", errorMessage);
        }

        return ValidationResult.success();
    }
}
