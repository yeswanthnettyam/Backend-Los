package com.los.controller;

import com.los.config.entity.FlowConfig;
import com.los.dto.config.FlowConfigDto;
import com.los.exception.ConfigNotFoundException;
import com.los.repository.FlowConfigRepository;
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
 * Controller for flow configuration management.
 */
@RestController
@RequestMapping("/api/v1/configs/flows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Flow Configuration", description = "Manage flow configurations")
public class FlowConfigController {

    private final FlowConfigRepository flowConfigRepository;

    @Operation(summary = "Get all flow configurations")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<List<FlowConfig>> getAllConfigs() {
        return ResponseEntity.ok(flowConfigRepository.findAll());
    }

    @Operation(summary = "Get flow configuration by ID")
    @GetMapping("/{configId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<FlowConfig> getConfigById(@PathVariable Long configId) {
        FlowConfig config = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Create new flow configuration")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FlowConfig> createConfig(@Valid @RequestBody FlowConfigDto dto) {
        String status = ConfigStatusValidator.validateAndSetDefault(dto.getStatus());
        
        FlowConfig config = FlowConfig.builder()
                .flowId(dto.getFlowId())
                .productCode(dto.getProductCode())
                .partnerCode(dto.getPartnerCode())
                .branchCode(dto.getBranchCode())
                .version(1)
                .status(status)
                .flowDefinition(dto.getFlowDefinition())
                .createdBy(dto.getCreatedBy())
                .build();

        log.debug("Creating flow config with status: {}", status);
        FlowConfig created = flowConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update flow configuration")
    @PutMapping("/{configId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FlowConfig> updateConfig(
            @PathVariable Long configId,
            @Valid @RequestBody FlowConfigDto dto) {
        FlowConfig config = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));

        String status = ConfigStatusValidator.validateIfProvided(dto.getStatus());
        if (status != null) {
            config.setStatus(status);
        }
        
        config.setFlowDefinition(dto.getFlowDefinition());
        config.setUpdatedBy(dto.getUpdatedBy());

        log.debug("Updating flow config {} with status: {}", configId, config.getStatus());
        FlowConfig updated = flowConfigRepository.save(config);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Clone flow configuration")
    @PostMapping("/{configId}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    @Transactional
    public ResponseEntity<FlowConfig> cloneConfig(@PathVariable Long configId) {
        FlowConfig original = flowConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Flow config not found: " + configId));

        FlowConfig clone = FlowConfig.builder()
                .flowId(original.getFlowId())
                .productCode(original.getProductCode())
                .partnerCode(original.getPartnerCode())
                .branchCode(original.getBranchCode())
                .version(1)
                .status("DRAFT")
                .flowDefinition(original.getFlowDefinition())
                .createdBy(original.getCreatedBy())
                .build();

        FlowConfig cloned = flowConfigRepository.save(clone);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @Operation(summary = "Delete flow configuration")
    @DeleteMapping("/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        if (!flowConfigRepository.existsById(configId)) {
            throw new ConfigNotFoundException("Flow config not found: " + configId);
        }
        flowConfigRepository.deleteById(configId);
        return ResponseEntity.noContent().build();
    }
}

