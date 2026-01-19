package com.los.validation;

import com.los.dto.runtime.ValidationErrorResponse;
import com.los.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core validation engine that executes validation rules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationEngine {

    private final List<ValidationRule> validationRules;

    /**
     * Validates form data against validation configuration.
     * If validationConfig is null or empty, validation is skipped (no errors thrown).
     * 
     * @param formData The form data to validate
     * @param validationConfig The validation rules configuration (can be null)
     * @throws ValidationException if validation fails
     */
    @SuppressWarnings("unchecked")
    public void validate(Map<String, Object> formData, Map<String, Object> validationConfig) {
        // Skip validation if config is null or empty
        if (validationConfig == null || validationConfig.isEmpty()) {
            log.debug("Validation config is null or empty. Skipping validation.");
            return;
        }

        List<ValidationErrorResponse.FieldError> errors = new ArrayList<>();

        // Extract fields validation rules
        Map<String, Object> fields = (Map<String, Object>) validationConfig.get("fields");
        if (fields == null || fields.isEmpty()) {
            log.debug("No fields defined in validation config. Skipping validation.");
            return;
        }

        // Validate each field
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldId = entry.getKey();
            Map<String, Object> fieldRules = (Map<String, Object>) entry.getValue();

            // Execute all applicable validation rules
            for (ValidationRule rule : validationRules) {
                if (rule.isApplicable(fieldRules)) {
                    ValidationResult result = rule.validate(fieldId, formData.get(fieldId), fieldRules, formData);
                    if (!result.isValid()) {
                        errors.add(ValidationErrorResponse.FieldError.builder()
                                .fieldId(fieldId)
                                .code(result.getErrorCode())
                                .message(result.getErrorMessage())
                                .build());
                    }
                }
            }
        }

        // Throw exception if there are errors
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}

