package com.los.validation.rules;

import com.los.validation.ValidationResult;
import com.los.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Validates multi-select field min/max count.
 */
@Component
public class MultiSelectValidationRule implements ValidationRule {

    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        return "MULTI_SELECT".equals(fieldRules.get("type"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, Map<String, Object> allFormData) {
        if (fieldValue == null) {
            return ValidationResult.success();
        }

        if (!(fieldValue instanceof List)) {
            return ValidationResult.failure("INVALID_TYPE", "Expected a list of values");
        }

        List<Object> values = (List<Object>) fieldValue;
        int count = values.size();

        if (fieldRules.containsKey("minCount")) {
            int min = ((Number) fieldRules.get("minCount")).intValue();
            if (count < min) {
                return ValidationResult.failure("MIN_COUNT", "Select at least " + min + " options");
            }
        }

        if (fieldRules.containsKey("maxCount")) {
            int max = ((Number) fieldRules.get("maxCount")).intValue();
            if (count > max) {
                return ValidationResult.failure("MAX_COUNT", "Select at most " + max + " options");
            }
        }

        return ValidationResult.success();
    }
}

