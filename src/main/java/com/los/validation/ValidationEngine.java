package com.los.validation;

import com.los.dto.runtime.ValidationErrorResponse;
import com.los.exception.ValidationException;
import com.los.service.ConfigResolutionService;
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
    private final ConfigResolutionService configResolutionService;

    /**
     * Validates form data against validation configuration.
     * If validationConfig is null or empty, validation is skipped (no errors thrown).
     * WebView fields are automatically ignored.
     * 
     * @param formData The form data to validate
     * @param validationConfig The validation rules configuration (can be null)
     * @throws ValidationException if validation fails
     */
    @SuppressWarnings("unchecked")
    public void validate(Map<String, Object> formData, Map<String, Object> validationConfig) {
        validate(formData, validationConfig, null, null, null, null);
    }

    /**
     * Validates form data against validation configuration.
     * WebView fields are automatically ignored based on screen config.
     * 
     * @param formData The form data to validate
     * @param validationConfig The validation rules configuration (can be null)
     * @param screenId The screen ID (for checking WebView fields)
     * @param productCode Product code (for resolving screen config)
     * @param partnerCode Partner code (for resolving screen config)
     * @param branchCode Branch code (for resolving screen config)
     * @throws ValidationException if validation fails
     */
    @SuppressWarnings("unchecked")
    public void validate(Map<String, Object> formData, Map<String, Object> validationConfig,
                        String screenId, String productCode, String partnerCode, String branchCode) {
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

        // Get WebView field IDs to ignore (if screenId is provided)
        java.util.Set<String> webViewFieldIds = getWebViewFieldIds(screenId, productCode, partnerCode, branchCode);

        // Validate each field (skip WebView fields)
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldId = entry.getKey();
            
            // Skip WebView fields - they don't affect validation or flow decisions
            if (webViewFieldIds.contains(fieldId)) {
                log.debug("Skipping validation for WebView field: {}", fieldId);
                continue;
            }

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

    /**
     * Get WebView field IDs from screen config.
     * Returns empty set if screen config cannot be resolved.
     */
    @SuppressWarnings("unchecked")
    private java.util.Set<String> getWebViewFieldIds(String screenId, String productCode, 
                                                     String partnerCode, String branchCode) {
        java.util.Set<String> webViewFieldIds = new java.util.HashSet<>();
        
        if (screenId == null || screenId.isEmpty()) {
            return webViewFieldIds; // Cannot determine WebView fields without screenId
        }

        try {
            Map<String, Object> screenConfig = configResolutionService.getScreenConfig(
                    screenId, productCode, partnerCode, branchCode);
            
            if (screenConfig == null) {
                log.debug("Screen config not found for screenId={}. Cannot determine WebView fields.", screenId);
                return webViewFieldIds;
            }

            Map<String, Object> uiConfig = (Map<String, Object>) screenConfig.get("uiConfig");
            if (uiConfig == null) {
                return webViewFieldIds;
            }

            List<Map<String, Object>> fields = (List<Map<String, Object>>) uiConfig.get("fields");
            if (fields == null || fields.isEmpty()) {
                return webViewFieldIds;
            }

            // Find all WebView fields
            for (Map<String, Object> field : fields) {
                String fieldType = (String) field.get("type");
                Map<String, Object> webView = (Map<String, Object>) field.get("webView");
                
                if ("WEBVIEW".equalsIgnoreCase(fieldType) || webView != null) {
                    String fieldId = (String) field.get("id");
                    if (fieldId != null) {
                        webViewFieldIds.add(fieldId);
                        log.debug("Found WebView field: {}", fieldId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to determine WebView fields for screenId={}. Error: {}", screenId, e.getMessage());
            // Continue with validation - better to validate than skip due to error
        }

        return webViewFieldIds;
    }
}

