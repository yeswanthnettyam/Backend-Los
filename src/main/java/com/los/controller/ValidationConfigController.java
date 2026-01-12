package com.los.controller;

import com.los.config.entity.ValidationConfig;
import com.los.dto.config.ValidationConfigDto;
import com.los.exception.ConfigNotFoundException;
import com.los.repository.ValidationConfigRepository;
import com.los.util.ConfigStatusValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for validation configuration management.
 */
@RestController
@RequestMapping("/api/v1/configs/validations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Validation Configuration", description = "Manage validation configurations")
public class ValidationConfigController {

    private final ValidationConfigRepository validationConfigRepository;

    @Operation(summary = "Get all validation configurations")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<List<ValidationConfig>> getAllConfigs() {
        return ResponseEntity.ok(validationConfigRepository.findAll());
    }

    @Operation(summary = "Get validation configuration by ID")
    @GetMapping("/{configId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<ValidationConfig> getConfigById(@PathVariable Long configId) {
        ValidationConfig config = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Create new validation configuration")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<ValidationConfig> createConfig(@Valid @RequestBody ValidationConfigDto dto) {
        // Set default status if not provided or validate if provided
        String status = ConfigStatusValidator.validateAndSetDefault(dto.getStatus());
        
        ValidationConfig config = ValidationConfig.builder()
                .screenId(dto.getScreenId())
                .productCode(dto.getProductCode())
                .partnerCode(dto.getPartnerCode())
                .branchCode(dto.getBranchCode())
                .version(1)
                .status(status)
                .validationRules(dto.getValidationRules())
                .createdBy(dto.getCreatedBy())
                .build();

        log.debug("Creating validation config with status: {}", status);
        ValidationConfig created = validationConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update validation configuration")
    @PutMapping("/{configId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<ValidationConfig> updateConfig(
            @PathVariable Long configId,
            @Valid @RequestBody ValidationConfigDto dto) {
        ValidationConfig config = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));

        // Validate status if provided
        String status = ConfigStatusValidator.validateIfProvided(dto.getStatus());
        if (status != null) {
            config.setStatus(status);
        }
        
        config.setValidationRules(dto.getValidationRules());
        config.setUpdatedBy(dto.getUpdatedBy());

        log.debug("Updating validation config {} with status: {}", configId, config.getStatus());
        ValidationConfig updated = validationConfigRepository.save(config);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Clone validation configuration")
    @PostMapping("/{configId}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<ValidationConfig> cloneConfig(@PathVariable Long configId) {
        ValidationConfig original = validationConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Validation config not found: " + configId));

        ValidationConfig clone = ValidationConfig.builder()
                .screenId(original.getScreenId())
                .productCode(original.getProductCode())
                .partnerCode(original.getPartnerCode())
                .branchCode(original.getBranchCode())
                .version(1)
                .status("DRAFT")
                .validationRules(original.getValidationRules())
                .createdBy(original.getCreatedBy())
                .build();

        ValidationConfig cloned = validationConfigRepository.save(clone);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @Operation(summary = "Delete validation configuration")
    @DeleteMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        if (!validationConfigRepository.existsById(configId)) {
            throw new ConfigNotFoundException("Validation config not found: " + configId);
        }
        validationConfigRepository.deleteById(configId);
        return ResponseEntity.noContent().build();
    }
}

