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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

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
     */
    @Transactional
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
        ScreenConfig activated = screenConfigRepository.save(config);
        
        log.info("Successfully activated ScreenConfig: {} (deprecated {} previous configs)", 
            configId, activeConfigs.size());
        
        return activated;
    }

    /**
     * Activate a FlowConfig.
     */
    @Transactional
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
        
        config.setStatus(ConfigStatus.ACTIVE.name());
        FlowConfig activated = flowConfigRepository.save(config);
        
        log.info("Successfully activated FlowConfig: {} (deprecated {} previous configs)", 
            configId, activeConfigs.size());
        
        return activated;
    }

    /**
     * Activate a FieldMappingConfig.
     */
    @Transactional
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
        FieldMappingConfig activated = fieldMappingConfigRepository.save(config);
        
        log.info("Successfully activated FieldMappingConfig: {} (deprecated {} previous configs)", 
            configId, activeConfigs.size());
        
        return activated;
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
     */
    @Transactional
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
        ValidationConfig activated = validationConfigRepository.save(config);
        
        log.info("Successfully activated ValidationConfig: {} (deprecated {} previous configs)", 
            configId, activeConfigs.size());
        
        return activated;
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
