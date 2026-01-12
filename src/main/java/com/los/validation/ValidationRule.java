package com.los.validation;

import java.util.Map;

/**
 * Interface for all validation rules.
 */
public interface ValidationRule {

    /**
     * Check if this rule is applicable based on field configuration.
     */
    boolean isApplicable(Map<String, Object> fieldRules);

    /**
     * Execute validation.
     * 
     * @param fieldId The field identifier
     * @param fieldValue The field value to validate
     * @param fieldRules The rules for this field
     * @param allFormData All form data (for cross-field validation)
     * @return Validation result
     */
    ValidationResult validate(String fieldId, Object fieldValue, Map<String, Object> fieldRules, Map<String, Object> allFormData);
}

