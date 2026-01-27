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
        ScreenConfig config = screenConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Screen config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
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
        
        validateScreenConfigCompleteness(config);
        
        List<ScreenConfig> activeConfigs = screenConfigRepository.findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
            config.getScreenId(),
            config.getProductCode(),
            config.getPartnerCode(),
            config.getBranchCode(),
            ConfigStatus.ACTIVE.name()
        );
        
        for (ScreenConfig activeConfig : activeConfigs) {
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            screenConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        Map<String, Object> uiConfig = config.getUiConfig();
        if (uiConfig != null) {
            Map<String, Object> updatedUiConfig = new HashMap<>(uiConfig);
            updatedUiConfig.put("status", ConfigStatus.ACTIVE.name());
            config.setUiConfig(updatedUiConfig);
        }
        
        entityManager.merge(config);
        entityManager.flush();
        
        if (uiConfig != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> uiConfigForDb = new HashMap<>(uiConfig);
                uiConfigForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(uiConfigForDb);
                
                entityManager.createNativeQuery(
                        "UPDATE SCREEN_CONFIGS SET UI_CONFIG = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
            } catch (Exception e) {
                log.error("Failed to update uiConfig JSON: {}", e.getMessage());
            }
        }
        
        entityManager.clear();
        
        return screenConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Screen config not found: " + configId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FlowConfig activateFlowConfig(Long configId) {
        FlowConfig config = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
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
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            flowConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        Map<String, Object> flowDefinition = config.getFlowDefinition();
        if (flowDefinition != null) {
            Map<String, Object> updatedFlowDefinition = new HashMap<>(flowDefinition);
            updatedFlowDefinition.put("status", ConfigStatus.ACTIVE.name());
            config.setFlowDefinition(updatedFlowDefinition);
        }
        
        entityManager.merge(config);
        entityManager.flush();
        
        if (flowDefinition != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> flowDefForDb = new HashMap<>(flowDefinition);
                flowDefForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(flowDefForDb);
                
                entityManager.createNativeQuery(
                        "UPDATE FLOW_CONFIGS SET FLOW_DEFINITION = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
            } catch (Exception e) {
                log.error("Failed to update flowDefinition JSON: {}", e.getMessage());
            }
        }
        
        entityManager.clear();
        
        return flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FieldMappingConfig activateFieldMappingConfig(Long configId) {
        FieldMappingConfig config = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
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
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            fieldMappingConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        Map<String, Object> mappings = config.getMappings();
        if (mappings != null) {
            Map<String, Object> updatedMappings = new HashMap<>(mappings);
            updatedMappings.put("status", ConfigStatus.ACTIVE.name());
            config.setMappings(updatedMappings);
        }
        
        entityManager.merge(config);
        entityManager.flush();
        
        if (mappings != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> mappingsForDb = new HashMap<>(mappings);
                mappingsForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(mappingsForDb);
                
                entityManager.createNativeQuery(
                        "UPDATE FIELD_MAPPING_CONFIGS SET MAPPINGS = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
            } catch (Exception e) {
                log.error("Failed to update mappings JSON: {}", e.getMessage());
            }
        }
        
        entityManager.clear();
        
        return fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));
    }

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
    }

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
    }

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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValidationConfig activateValidationConfig(Long configId) {
        ValidationConfig config = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));
        
        if (ConfigStatus.ACTIVE.name().equals(config.getStatus())) {
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
            activeConfig.setStatus(ConfigStatus.DEPRECATED.name());
            validationConfigRepository.save(activeConfig);
        }
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        
        Map<String, Object> validationRules = config.getValidationRules();
        if (validationRules != null) {
            Map<String, Object> updatedValidationRules = new HashMap<>(validationRules);
            updatedValidationRules.put("status", ConfigStatus.ACTIVE.name());
            config.setValidationRules(updatedValidationRules);
        }
        
        entityManager.merge(config);
        entityManager.flush();
        
        if (validationRules != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> validationRulesForDb = new HashMap<>(validationRules);
                validationRulesForDb.put("status", ConfigStatus.ACTIVE.name());
                String updatedJson = objectMapper.writeValueAsString(validationRulesForDb);
                
                entityManager.createNativeQuery(
                        "UPDATE VALIDATION_CONFIGS SET VALIDATION_RULES = ? WHERE CONFIG_ID = ?")
                        .setParameter(1, updatedJson)
                        .setParameter(2, configId)
                        .executeUpdate();
                entityManager.flush();
            } catch (Exception e) {
                log.error("Failed to update validationRules JSON: {}", e.getMessage());
            }
        }
        
        entityManager.clear();
        
        return validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));
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
    }
}
