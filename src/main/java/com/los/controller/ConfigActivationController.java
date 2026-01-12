package com.los.controller;

import com.los.config.entity.FieldMappingConfig;
import com.los.config.entity.FlowConfig;
import com.los.config.entity.ScreenConfig;
import com.los.config.entity.ValidationConfig;
import com.los.service.ConfigActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Common activation API for all configuration types.
 * 
 * Provides unified activation endpoints:
 * - POST /configs/screens/{configId}/activate
 * - POST /configs/flows/{configId}/activate
 * - POST /configs/mappings/{configId}/activate
 * - POST /configs/validations/{configId}/activate
 * 
 * Activation Rules:
 * 1. Only DRAFT configs can be activated
 * 2. Only ONE ACTIVE config per scope
 * 3. Previous ACTIVE config is automatically DEPRECATED
 * 4. Atomic transaction ensures consistency
 */
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Config Activation", description = "Common activation API for all config types")
public class ConfigActivationController {

    private final ConfigActivationService configActivationService;

    @Operation(
        summary = "Activate a Screen Config",
        description = "Activates a DRAFT screen config and deprecates previous ACTIVE config in same scope. " +
                     "Scope = screenId + productCode + partnerCode + branchCode"
    )
    @PostMapping("/screens/{configId}/activate")
    public ResponseEntity<ScreenConfig> activateScreenConfig(@PathVariable Long configId) {
        log.info("Activation request for ScreenConfig: {}", configId);
        ScreenConfig activated = configActivationService.activateScreenConfig(configId);
        return ResponseEntity.ok(activated);
    }

    @Operation(
        summary = "Activate a Flow Config",
        description = "Activates a DRAFT flow config and deprecates previous ACTIVE config in same scope. " +
                     "Scope = flowId + productCode + partnerCode + branchCode"
    )
    @PostMapping("/flows/{configId}/activate")
    public ResponseEntity<FlowConfig> activateFlowConfig(@PathVariable Long configId) {
        log.info("Activation request for FlowConfig: {}", configId);
        FlowConfig activated = configActivationService.activateFlowConfig(configId);
        return ResponseEntity.ok(activated);
    }

    @Operation(
        summary = "Activate a Field Mapping Config",
        description = "Activates a DRAFT field mapping config and deprecates previous ACTIVE config in same scope. " +
                     "Scope = screenId + productCode + partnerCode + branchCode"
    )
    @PostMapping("/field-mappings/{configId}/activate")
    public ResponseEntity<FieldMappingConfig> activateFieldMappingConfig(@PathVariable Long configId) {
        log.info("Activation request for FieldMappingConfig: {}", configId);
        FieldMappingConfig activated = configActivationService.activateFieldMappingConfig(configId);
        return ResponseEntity.ok(activated);
    }

    @Operation(
        summary = "Activate a Validation Config",
        description = "Activates a DRAFT validation config and deprecates previous ACTIVE config in same scope. " +
                     "Scope = screenId + productCode + partnerCode + branchCode"
    )
    @PostMapping("/validations/{configId}/activate")
    public ResponseEntity<ValidationConfig> activateValidationConfig(@PathVariable Long configId) {
        log.info("Activation request for ValidationConfig: {}", configId);
        ValidationConfig activated = configActivationService.activateValidationConfig(configId);
        return ResponseEntity.ok(activated);
    }
}
