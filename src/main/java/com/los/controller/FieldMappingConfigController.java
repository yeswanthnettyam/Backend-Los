package com.los.controller;

import com.los.config.entity.FieldMappingConfig;
import com.los.dto.config.FieldMappingConfigDto;
import com.los.exception.ConfigNotFoundException;
import com.los.repository.FieldMappingConfigRepository;
import com.los.util.ConfigStatusValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// DISABLED FOR TESTING: Authentication removed
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for field mapping configuration management.
 */
@RestController
@RequestMapping("/api/v1/configs/field-mappings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Field Mapping Configuration", description = "Manage field mapping configurations")
public class FieldMappingConfigController {

    private final FieldMappingConfigRepository fieldMappingConfigRepository;

    @Operation(summary = "Get all field mapping configurations")
    @GetMapping
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<List<FieldMappingConfig>> getAllConfigs() {
        return ResponseEntity.ok(fieldMappingConfigRepository.findAll());
    }

    @Operation(summary = "Get field mapping configuration by ID")
    @GetMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<FieldMappingConfig> getConfigById(@PathVariable Long configId) {
        FieldMappingConfig config = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Create new field mapping configuration")
    @PostMapping
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FieldMappingConfig> createConfig(@Valid @RequestBody FieldMappingConfigDto dto) {
        String status = ConfigStatusValidator.validateAndSetDefault(dto.getStatus());
        
        FieldMappingConfig config = FieldMappingConfig.builder()
                .screenId(dto.getScreenId())
                .productCode(dto.getProductCode())
                .partnerCode(dto.getPartnerCode())
                .branchCode(dto.getBranchCode())
                .version(1)
                .status(status)
                .mappings(dto.getMappings())
                .createdBy(dto.getCreatedBy())
                .build();

        log.debug("Creating field mapping config with status: {}", status);
        FieldMappingConfig created = fieldMappingConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update field mapping configuration")
    @PutMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FieldMappingConfig> updateConfig(
            @PathVariable Long configId,
            @Valid @RequestBody FieldMappingConfigDto dto) {
        FieldMappingConfig config = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));

        String status = ConfigStatusValidator.validateIfProvided(dto.getStatus());
        if (status != null) {
            config.setStatus(status);
        }
        
        config.setMappings(dto.getMappings());
        config.setUpdatedBy(dto.getUpdatedBy());

        log.debug("Updating field mapping config {} with status: {}", configId, config.getStatus());
        FieldMappingConfig updated = fieldMappingConfigRepository.save(config);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Clone field mapping configuration")
    @PostMapping("/{configId}/clone")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FieldMappingConfig> cloneConfig(@PathVariable Long configId) {
        FieldMappingConfig original = fieldMappingConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Field mapping config not found: " + configId));

        FieldMappingConfig clone = FieldMappingConfig.builder()
                .screenId(original.getScreenId())
                .productCode(original.getProductCode())
                .partnerCode(original.getPartnerCode())
                .branchCode(original.getBranchCode())
                .version(1)
                .status("DRAFT")
                .mappings(original.getMappings())
                .createdBy(original.getCreatedBy())
                .build();

        FieldMappingConfig cloned = fieldMappingConfigRepository.save(clone);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @Operation(summary = "Delete field mapping configuration")
    @DeleteMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        if (!fieldMappingConfigRepository.existsById(configId)) {
            throw new ConfigNotFoundException("Field mapping config not found: " + configId);
        }
        fieldMappingConfigRepository.deleteById(configId);
        return ResponseEntity.noContent().build();
    }
}

