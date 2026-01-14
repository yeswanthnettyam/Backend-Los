package com.los.service;

import com.los.config.ConfigStatus;
import com.los.config.entity.FieldMappingConfig;
import com.los.config.entity.FlowConfig;
import com.los.config.entity.ScreenConfig;
import com.los.config.entity.ValidationConfig;
import com.los.dto.runtime.ValidationErrorResponse;
import com.los.exception.ConfigNotFoundException;
import com.los.exception.ValidationException;
import com.los.repository.FieldMappingConfigRepository;
import com.los.repository.FlowConfigRepository;
import com.los.repository.ScreenConfigRepository;
import com.los.repository.ValidationConfigRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for activating configurations across all config types.
 * 
 * Ensures:
 * - Only ONE ACTIVE config per scope
 * - Atomic activation (deprecate old → activate new)
 * - Validation before activation
 * - Audit trail preservation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigActivationService {

    private final ScreenConfigRepository screenConfigRepository;
    private final FlowConfigRepository flowConfigRepository;
    private final FieldMappingConfigRepository fieldMappingConfigRepository;
    private final ValidationConfigRepository validationConfigRepository;
    private final EntityManager entityManager;

    /**
     * Activate a ScreenConfig.
     * 
     * Rules:
     * - Only ONE ACTIVE config per screenId + scope
     * - Deprecates previous ACTIVE config
     * - Validates completeness
     * 
     * @param configId the config ID to activate
     * @return activated config
     * Uses REQUIRES_NEW to ensure transaction commits immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScreenConfig activateScreenConfig(Long configId) {
        log.info("Activating ScreenConfig: {}", configId);
        
        // 1. Get config to activate
        ScreenConfig config = screenConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Screen config not found: " + configId));
        
        // 2. Validate current status
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
            log.warn("Config {} is already ACTIVE", configId);
            return config; // Idempotent
        }
        
        if (!ConfigStatus.DRAFT.name().equals(config.getStatus())) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_STATUS")
                    .message(String.format("Cannot activate config with status %s. Only DRAFT configs can be activated.", 
                        config.getStatus()))
                    .build()
            ));
        }
        
        // 3. Validate completeness
        validateScreenConfigCompleteness(config);
        
        // 4. Find and deprecate previous ACTIVE config in same scope
        List<ScreenConfig> activeConfigs = screenConfigRepository.findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
            config.getScreenId(),
            config.getProductCode(),
            config.getPartnerCode(),
            config.getBranchCode(),
            ConfigStatus.ACTIVE.name()
        );
        
        for (ScreenConfig activeConfig : activeConfigs) {
            log.info("Deprecating previous ACTIVE ScreenConfig: {}", activeConfig.getConfigId());
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            screenConfigRepository.save(activeConfig);
        }
        
        // 5. Activate new config
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        // CRITICAL: Also update status inside uiConfig JSON to match top-level status
        // The frontend may read status from uiConfig, so we must keep them in sync
        Map<String, Object> uiConfig = config.getUiConfig();
        if (uiConfig != null) {
            // Create a new HashMap to ensure Hibernate detects the change
            Map<String, Object> updatedUiConfig = new HashMap<>(uiConfig);
            updatedUiConfig.put("status", ConfigStatus.ACTIVE.name());
            config.setUiConfig(updatedUiConfig); // Set it back to trigger converter
            log.debug("Updated status inside uiConfig to ACTIVE and set back on entity");
        }
        
        ScreenConfig merged = entityManager.merge(config);
        entityManager.flush(); // Force immediate database write
        
        // Also update uiConfig JSON directly in database using SQL to ensure it's persisted
        if (uiConfig != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> uiConfigForDb = new HashMap<>(uiConfig);
                uiConfigForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(uiConfigForDb);
                
                // Update the JSON directly in database
                int jsonUpdated = entityManager.createNativeQuery(
                        "UPDATE SCREEN_CONFIGS SET UI_CONFIG = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
                log.debug("Direct SQL update of uiConfig JSON: {} rows updated", jsonUpdated);
            } catch (Exception e) {
                log.error("Failed to update uiConfig JSON directly: {}", e.getMessage(), e);
            }
        }
        
        entityManager.clear(); // Clear persistence context
        
        // Fetch fresh entity from database
        ScreenConfig freshConfig = screenConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Screen config not found: " + configId));
        
        log.info("Successfully activated ScreenConfig: {} (deprecated {} previous configs). Status: {}", 
            configId, activeConfigs.size(), freshConfig.getStatus());
        
        // Verify status inside uiConfig is also ACTIVE
        Map<String, Object> freshUiConfig = freshConfig.getUiConfig();
        if (freshUiConfig != null) {
            Object statusInUiConfig = freshUiConfig.get("status");
            if (!ConfigStatus.ACTIVE.name().equals(statusInUiConfig)) {
                log.warn("WARNING: Status inside uiConfig is '{}' but should be 'ACTIVE'. Frontend may show incorrect status.", 
                    statusInUiConfig);
            } else {
                log.debug("Verified: Status inside uiConfig is also ACTIVE");
            }
        }
        
        return freshConfig;
    }

    /**
     * Activate a FlowConfig.
     * Uses REQUIRES_NEW to ensure transaction commits immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FlowConfig activateFlowConfig(Long configId) {
        log.info("Activating FlowConfig: {}", configId);
        
        FlowConfig config = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
            log.warn("Config {} is already ACTIVE", configId);
            return config;
        }
        
        if (!ConfigStatus.DRAFT.name().equals(config.getStatus())) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_STATUS")
                    .message(String.format("Cannot activate config with status %s. Only DRAFT configs can be activated.", 
                        config.getStatus()))
                    .build()
            ));
        }
        
        validateFlowConfigCompleteness(config);
        
        List<FlowConfig> activeConfigs = flowConfigRepository.findByFlowIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
            config.getFlowId(),
            config.getProductCode(),
            config.getPartnerCode(),
            config.getBranchCode(),
            ConfigStatus.ACTIVE.name()
        );
        
        for (FlowConfig activeConfig : activeConfigs) {
            log.info("Deprecating previous ACTIVE FlowConfig: {}", activeConfig.getConfigId());
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            flowConfigRepository.save(activeConfig);
        }
        
        log.debug("Setting status to ACTIVE for FlowConfig: {}", configId);
        config.setStatus(ConfigStatus.ACTIVE.name());
        log.debug("Status set to: {}", config.getStatus());
        
        // CRITICAL: Also update status inside flowDefinition JSON to match top-level status
        // The frontend reads status from flowDefinition, so we must keep them in sync
        // We need to create a new Map and set it back to trigger Hibernate's dirty checking
        Map<String, Object> flowDefinition = config.getFlowDefinition();
        if (flowDefinition != null) {
            // Create a new HashMap to ensure Hibernate detects the change
            Map<String, Object> updatedFlowDefinition = new HashMap<>(flowDefinition);
            updatedFlowDefinition.put("status", ConfigStatus.ACTIVE.name());
            config.setFlowDefinition(updatedFlowDefinition); // Set it back to trigger converter
            log.debug("Updated status inside flowDefinition to ACTIVE and set back on entity");
        }
        
        // Use merge to ensure the entity is managed and changes are tracked
        FlowConfig merged = entityManager.merge(config);
        log.debug("After merge, status is: {}", merged.getStatus());
        
        entityManager.flush(); // Force immediate database write
        
        // Also update flowDefinition JSON directly in database using SQL to ensure it's persisted
        // This is a fallback to ensure the JSON is definitely updated even if Hibernate doesn't detect the change
        if (flowDefinition != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> flowDefForDb = new HashMap<>(flowDefinition);
                flowDefForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(flowDefForDb);
                
                // Update the JSON directly in database
                int jsonUpdated = entityManager.createNativeQuery(
                        "UPDATE FLOW_CONFIGS SET FLOW_DEFINITION = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
                log.debug("Direct SQL update of flowDefinition JSON: {} rows updated", jsonUpdated);
            } catch (Exception e) {
                log.error("Failed to update flowDefinition JSON directly: {}", e.getMessage(), e);
            }
        }
        
        // Verify with direct SQL query to ensure database was updated
        // H2 uses uppercase table names in MySQL mode
        String statusFromDb = (String) entityManager.createNativeQuery(
                "SELECT STATUS FROM FLOW_CONFIGS WHERE CONFIG_ID = ?")
                .setParameter(1, configId)
                .getSingleResult();
        log.debug("Direct SQL query confirms status in DB: {}", statusFromDb);
        
        if (!ConfigStatus.ACTIVE.name().equals(statusFromDb)) {
            log.error("CRITICAL: Database status mismatch! Expected ACTIVE but DB has: {}. Attempting direct update.", statusFromDb);
            // Force direct SQL update as fallback
            int updated = entityManager.createNativeQuery(
                    "UPDATE FLOW_CONFIGS SET STATUS = ? WHERE CONFIG_ID = ?")
                    .setParameter(1, ConfigStatus.ACTIVE.name())
                    .setParameter(2, configId)
                    .executeUpdate();
            entityManager.flush();
            log.warn("Forced direct SQL update to ACTIVE for config {} ({} rows updated)", configId, updated);
        }
        
        // Clear persistence context to force fresh fetch from database
        entityManager.clear();
        
        // Fetch fresh entity from database to ensure we return the latest committed state
        // This bypasses any Hibernate first-level cache issues
        FlowConfig freshConfig = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));
        
        log.info("Successfully activated FlowConfig: {} (deprecated {} previous configs). Status from DB: {}", 
            configId, activeConfigs.size(), freshConfig.getStatus());
        
        if (!ConfigStatus.ACTIVE.name().equals(freshConfig.getStatus())) {
            log.error("CRITICAL: Status mismatch after fresh fetch! Expected ACTIVE but got: {}. Config ID: {}", 
                freshConfig.getStatus(), configId);
        }
        
        // Verify status inside flowDefinition is also ACTIVE
        Map<String, Object> freshFlowDefinition = freshConfig.getFlowDefinition();
        if (freshFlowDefinition != null) {
            Object statusInFlowDef = freshFlowDefinition.get("status");
            if (!ConfigStatus.ACTIVE.name().equals(statusInFlowDef)) {
                log.warn("WARNING: Status inside flowDefinition is '{}' but should be 'ACTIVE'. Frontend may show incorrect status.", 
                    statusInFlowDef);
            } else {
                log.debug("Verified: Status inside flowDefinition is also ACTIVE");
            }
        }
        
        return freshConfig;
    }

    /**
     * Activate a FieldMappingConfig.
     * Uses REQUIRES_NEW to ensure transaction commits immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FieldMappingConfig activateFieldMappingConfig(Long configId) {
        log.info("Activating FieldMappingConfig: {}", configId);
        
        FieldMappingConfig config = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
            log.warn("Config {} is already ACTIVE", configId);
            return config;
        }
        
        if (!ConfigStatus.DRAFT.name().equals(config.getStatus())) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_STATUS")
                    .message(String.format("Cannot activate config with status %s. Only DRAFT configs can be activated.", 
                        config.getStatus()))
                    .build()
            ));
        }
        
        validateFieldMappingConfigCompleteness(config);
        
        List<FieldMappingConfig> activeConfigs = fieldMappingConfigRepository.findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
            config.getScreenId(),
            config.getProductCode(),
            config.getPartnerCode(),
            config.getBranchCode(),
            ConfigStatus.ACTIVE.name()
        );
        
        for (FieldMappingConfig activeConfig : activeConfigs) {
            log.info("Deprecating previous ACTIVE FieldMappingConfig: {}", activeConfig.getConfigId());
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            fieldMappingConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        // CRITICAL: Also update status inside mappings JSON to match top-level status
        // The frontend may read status from mappings, so we must keep them in sync
        Map<String, Object> mappings = config.getMappings();
        if (mappings != null) {
            // Create a new HashMap to ensure Hibernate detects the change
            Map<String, Object> updatedMappings = new HashMap<>(mappings);
            updatedMappings.put("status", ConfigStatus.ACTIVE.name());
            config.setMappings(updatedMappings); // Set it back to trigger converter
            log.debug("Updated status inside mappings to ACTIVE and set back on entity");
        }
        
        FieldMappingConfig merged = entityManager.merge(config);
        entityManager.flush(); // Force immediate database write
        
        // Also update mappings JSON directly in database using SQL to ensure it's persisted
        if (mappings != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> mappingsForDb = new HashMap<>(mappings);
                mappingsForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(mappingsForDb);
                
                // Update the JSON directly in database
                int jsonUpdated = entityManager.createNativeQuery(
                        "UPDATE FIELD_MAPPING_CONFIGS SET MAPPINGS = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
                log.debug("Direct SQL update of mappings JSON: {} rows updated", jsonUpdated);
            } catch (Exception e) {
                log.error("Failed to update mappings JSON directly: {}", e.getMessage(), e);
            }
        }
        
        entityManager.clear(); // Clear persistence context
        
        // Fetch fresh entity from database
        FieldMappingConfig freshConfig = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));
        
        log.info("Successfully activated FieldMappingConfig: {} (deprecated {} previous configs). Status: {}", 
            configId, activeConfigs.size(), freshConfig.getStatus());
        
        // Verify status inside mappings is also ACTIVE
        Map<String, Object> freshMappings = freshConfig.getMappings();
        if (freshMappings != null) {
            Object statusInMappings = freshMappings.get("status");
            if (!ConfigStatus.ACTIVE.name().equals(statusInMappings)) {
                log.warn("WARNING: Status inside mappings is '{}' but should be 'ACTIVE'. Frontend may show incorrect status.", 
                    statusInMappings);
            } else {
                log.debug("Verified: Status inside mappings is also ACTIVE");
            }
        }
        
        return freshConfig;
    }

    /**
     * Validate ScreenConfig completeness before activation.
     */
    private void validateScreenConfigCompleteness(ScreenConfig config) {
        if (config.getScreenId() == null || config.getScreenId().isBlank()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("screenId")
                    .code("REQUIRED")
                    .message("ScreenId is required for activation")
                    .build()
            ));
        }
        if (config.getUiConfig() == null || config.getUiConfig().isEmpty()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("uiConfig")
                    .code("REQUIRED")
                    .message("UI config cannot be empty for activation")
                    .build()
            ));
        }
        // Add more validation rules as needed
    }

    /**
     * Validate FlowConfig completeness before activation.
     */
    private void validateFlowConfigCompleteness(FlowConfig config) {
        if (config.getFlowId() == null || config.getFlowId().isBlank()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("flowId")
                    .code("REQUIRED")
                    .message("FlowId is required for activation")
                    .build()
            ));
        }
        if (config.getFlowDefinition() == null || config.getFlowDefinition().isEmpty()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("flowDefinition")
                    .code("REQUIRED")
                    .message("Flow definition cannot be empty for activation")
                    .build()
            ));
        }
        // Add more validation rules as needed
    }

    /**
     * Validate FieldMappingConfig completeness before activation.
     */
    private void validateFieldMappingConfigCompleteness(FieldMappingConfig config) {
        if (config.getScreenId() == null || config.getScreenId().isBlank()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("screenId")
                    .code("REQUIRED")
                    .message("ScreenId is required for activation")
                    .build()
            ));
        }
        if (config.getMappings() == null || config.getMappings().isEmpty()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("mappings")
                    .code("REQUIRED")
                    .message("Mappings cannot be empty for activation")
                    .build()
            ));
        }
        // Add more validation rules as needed
    }

    /**
     * Activate a ValidationConfig.
     * Uses REQUIRES_NEW to ensure transaction commits immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValidationConfig activateValidationConfig(Long configId) {
        log.info("Activating ValidationConfig: {}", configId);
        
        ValidationConfig config = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
            log.warn("Config {} is already ACTIVE", configId);
            return config;
        }
        
        if (!ConfigStatus.DRAFT.name().equals(config.getStatus())) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("status")
                    .code("INVALID_STATUS")
                    .message(String.format("Cannot activate config with status %s. Only DRAFT configs can be activated.", 
                        config.getStatus()))
                    .build()
            ));
        }
        
        validateValidationConfigCompleteness(config);
        
        List<ValidationConfig> activeConfigs = validationConfigRepository.findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
            config.getScreenId(),
            config.getProductCode(),
            config.getPartnerCode(),
            config.getBranchCode(),
            ConfigStatus.ACTIVE.name()
        );
        
        for (ValidationConfig activeConfig : activeConfigs) {
            log.info("Deprecating previous ACTIVE ValidationConfig: {}", activeConfig.getConfigId());
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            validationConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        // CRITICAL: Also update status inside validationRules JSON to match top-level status
        // The frontend may read status from validationRules, so we must keep them in sync
        Map<String, Object> validationRules = config.getValidationRules();
        if (validationRules != null) {
            // Create a new HashMap to ensure Hibernate detects the change
            Map<String, Object> updatedValidationRules = new HashMap<>(validationRules);
            updatedValidationRules.put("status", ConfigStatus.ACTIVE.name());
            config.setValidationRules(updatedValidationRules); // Set it back to trigger converter
            log.debug("Updated status inside validationRules to ACTIVE and set back on entity");
        }
        
        ValidationConfig merged = entityManager.merge(config);
        entityManager.flush(); // Force immediate database write
        
        // Also update validationRules JSON directly in database using SQL to ensure it's persisted
        if (validationRules != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> validationRulesForDb = new HashMap<>(validationRules);
                validationRulesForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(validationRulesForDb);
                
                // Update the JSON directly in database
                int jsonUpdated = entityManager.createNativeQuery(
                        "UPDATE VALIDATION_CONFIGS SET VALIDATION_RULES = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
                log.debug("Direct SQL update of validationRules JSON: {} rows updated", jsonUpdated);
            } catch (Exception e) {
                log.error("Failed to update validationRules JSON directly: {}", e.getMessage(), e);
            }
        }
        
        entityManager.clear(); // Clear persistence context
        
        // Fetch fresh entity from database
        ValidationConfig freshConfig = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));
        
        log.info("Successfully activated ValidationConfig: {} (deprecated {} previous configs). Status: {}", 
            configId, activeConfigs.size(), freshConfig.getStatus());
        
        // Verify status inside validationRules is also ACTIVE
        Map<String, Object> freshValidationRules = freshConfig.getValidationRules();
        if (freshValidationRules != null) {
            Object statusInValidationRules = freshValidationRules.get("status");
            if (!ConfigStatus.ACTIVE.name().equals(statusInValidationRules)) {
                log.warn("WARNING: Status inside validationRules is '{}' but should be 'ACTIVE'. Frontend may show incorrect status.", 
                    statusInValidationRules);
            } else {
                log.debug("Verified: Status inside validationRules is also ACTIVE");
            }
        }
        
        return freshConfig;
    }

    /**
     * Validate ValidationConfig completeness before activation.
     */
    private void validateValidationConfigCompleteness(ValidationConfig config) {
        if (config.getScreenId() == null || config.getScreenId().isBlank()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("screenId")
                    .code("REQUIRED")
                    .message("ScreenId is required for activation")
                    .build()
            ));
        }
        if (config.getValidationRules() == null || config.getValidationRules().isEmpty()) {
            throw new ValidationException(Collections.singletonList(
                ValidationErrorResponse.FieldError.builder()
                    .fieldId("validationRules")
                    .code("REQUIRED")
                    .message("Validation rules cannot be empty for activation")
                    .build()
            ));
        }
        // Add more validation rules as needed
    }
}
