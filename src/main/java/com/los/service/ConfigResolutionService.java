package com.los.service;

import com.los.config.ConfigStatus;
import com.los.config.entity.FieldMappingConfig;
import com.los.config.entity.FlowConfig;
import com.los.config.entity.ScreenConfig;
import com.los.config.entity.ValidationConfig;
import com.los.exception.ConfigNotFoundException;
import com.los.repository.FieldMappingConfigRepository;
import com.los.repository.FlowConfigRepository;
import com.los.repository.ScreenConfigRepository;
import com.los.repository.ValidationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for resolving ACTIVE configurations at runtime.
 * 
 * CRITICAL RULES:
 * - Only resolves ACTIVE configs
 * - DRAFT configs are NEVER returned
 * - Used for creating immutable FlowSnapshots
 * - Scope resolution: branch > partner > product
 * 
 * Runtime Behavior:
 * - New applications get latest ACTIVE configs
 * - Existing applications use their FlowSnapshot (immutable)
 * - Config updates only affect NEW applications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigResolutionService {

    private final ScreenConfigRepository screenConfigRepository;
    private final ValidationConfigRepository validationConfigRepository;
    private final FieldMappingConfigRepository fieldMappingConfigRepository;
    private final FlowConfigRepository flowConfigRepository;

    /**
     * Resolve ACTIVE screen config using scope resolution logic.
     * Returns only the UI config data.
     * 
     * @return UI config map (NEVER returns DRAFT configs)
     */
    public Map<String, Object> getScreenConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        ScreenConfig config = resolveActiveScreenConfig(screenId, productCode, partnerCode, branchCode);
        return config.getUiConfig();
    }
    
    /**
     * Resolve ACTIVE screen config entity.
     * Used for FlowSnapshot creation to capture full config details.
     * 
     * @return ScreenConfig entity (ACTIVE status only)
     */
    public ScreenConfig resolveActiveScreenConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null or blank");
        }
        // Normalize empty strings to null for scope parameters
        productCode = (productCode != null && productCode.isBlank()) ? null : productCode;
        partnerCode = (partnerCode != null && partnerCode.isBlank()) ? null : partnerCode;
        branchCode = (branchCode != null && branchCode.isBlank()) ? null : branchCode;
        
        List<ScreenConfig> configs = screenConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        
        if (configs.isEmpty()) {
            throw new ConfigNotFoundException(
                String.format("No ACTIVE screen config found for screenId=%s, scope=%s/%s/%s", 
                    screenId, productCode, partnerCode, branchCode)
            );
        }
        
        ScreenConfig config = configs.get(0); // First match wins (branch > partner > product)
        
        // Safety check: ensure we never return DRAFT configs
        if (!ConfigStatus.isRuntimeUsable(config.getStatus())) {
            log.error("CRITICAL: Non-ACTIVE config returned from findByScope: {}", config.getConfigId());
            throw new ConfigNotFoundException("No ACTIVE config available for: " + screenId);
        }
        
        return config;
    }

    /**
     * Resolve validation config using scope resolution logic.
     * Returns null if no validation config is found (validation will be skipped).
     * 
     * @return Validation rules map, or null if not found
     */
    public Map<String, Object> getValidationConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        if (screenId == null || screenId.isBlank()) {
            log.warn("screenId is null or blank, cannot resolve validation config");
            return null;
        }
        // Normalize empty strings to null for scope parameters
        productCode = (productCode != null && productCode.isBlank()) ? null : productCode;
        partnerCode = (partnerCode != null && partnerCode.isBlank()) ? null : partnerCode;
        branchCode = (branchCode != null && branchCode.isBlank()) ? null : branchCode;
        
        List<ValidationConfig> configs = validationConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        
        if (configs.isEmpty()) {
            log.debug("No validation config found for screenId={}, scope={}/{}/{}. Validation will be skipped.", 
                    screenId, productCode, partnerCode, branchCode);
            return null;
        }
        
        ValidationConfig config = configs.get(0);
        
        // Only return ACTIVE configs
        if (!ConfigStatus.isRuntimeUsable(config.getStatus())) {
            log.debug("Validation config found but not ACTIVE (status={}) for screenId={}. Validation will be skipped.", 
                    config.getStatus(), screenId);
            return null;
        }
        
        return config.getValidationRules();
    }

    /**
     * Resolve field mapping config using scope resolution logic.
     * Returns only the mappings data.
     * 
     * @return Mappings map (NEVER returns DRAFT configs)
     */
    public Map<String, Object> getFieldMappingConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        FieldMappingConfig config = resolveActiveFieldMappingConfig(screenId, productCode, partnerCode, branchCode);
        return config.getMappings();
    }
    
    /**
     * Resolve ACTIVE field mapping config entity.
     * Used for FlowSnapshot creation.
     * 
     * @return FieldMappingConfig entity (ACTIVE status only)
     */
    public FieldMappingConfig resolveActiveFieldMappingConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId cannot be null or blank");
        }
        // Normalize empty strings to null for scope parameters
        productCode = (productCode != null && productCode.isBlank()) ? null : productCode;
        partnerCode = (partnerCode != null && partnerCode.isBlank()) ? null : partnerCode;
        branchCode = (branchCode != null && branchCode.isBlank()) ? null : branchCode;
        
        List<FieldMappingConfig> configs = fieldMappingConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        
        if (configs.isEmpty()) {
            throw new ConfigNotFoundException(
                String.format("No ACTIVE field mapping config found for screenId=%s, scope=%s/%s/%s", 
                    screenId, productCode, partnerCode, branchCode)
            );
        }
        
        FieldMappingConfig config = configs.get(0);
        
        if (!ConfigStatus.isRuntimeUsable(config.getStatus())) {
            log.error("CRITICAL: Non-ACTIVE config returned from findByScope: {}", config.getConfigId());
            throw new ConfigNotFoundException("No ACTIVE config available for: " + screenId);
        }
        
        return config;
    }
    
    /**
     * Resolve ACTIVE flow config entity.
     * Used for FlowSnapshot creation.
     * 
     * @return FlowConfig entity (ACTIVE status only)
     */
    public FlowConfig resolveActiveFlowConfig(String flowId, String productCode, String partnerCode, String branchCode) {
        if (flowId == null || flowId.isBlank()) {
            throw new IllegalArgumentException("flowId cannot be null or blank");
        }
        // Normalize empty strings to null for scope parameters
        productCode = (productCode != null && productCode.isBlank()) ? null : productCode;
        partnerCode = (partnerCode != null && partnerCode.isBlank()) ? null : partnerCode;
        branchCode = (branchCode != null && branchCode.isBlank()) ? null : branchCode;
        
        List<FlowConfig> configs = flowConfigRepository.findByScope(flowId, productCode, partnerCode, branchCode);
        
        if (configs.isEmpty()) {
            throw new ConfigNotFoundException(
                String.format("No ACTIVE flow config found for flowId=%s, scope=%s/%s/%s", 
                    flowId, productCode, partnerCode, branchCode)
            );
        }
        
        FlowConfig config = configs.get(0);
        
        if (!ConfigStatus.isRuntimeUsable(config.getStatus())) {
            log.error("CRITICAL: Non-ACTIVE config returned from findByScope: {}", config.getConfigId());
            throw new ConfigNotFoundException("No ACTIVE config available for: " + flowId);
        }
        
        return config;
    }
}

