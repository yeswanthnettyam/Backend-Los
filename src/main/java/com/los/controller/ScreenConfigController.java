package com.los.controller;

import com.los.config.entity.ScreenConfig;
import com.los.dto.config.ScreenConfigDto;
import com.los.service.ScreenConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// DISABLED FOR TESTING: Authentication removed
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for screen configuration management.
 */
@RestController
@RequestMapping("/api/v1/configs/screens")
@RequiredArgsConstructor
@Tag(name = "Screen Configuration", description = "Manage screen configurations")
public class ScreenConfigController {

    private final ScreenConfigService screenConfigService;

    @Operation(summary = "Get all screen configurations")
    @GetMapping
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<List<ScreenConfig>> getAllConfigs() {
        return ResponseEntity.ok(screenConfigService.getAllConfigs());
    }

    @Operation(summary = "Get screen configuration by ID")
    @GetMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR', 'VIEWER')")
    public ResponseEntity<ScreenConfig> getConfigById(@PathVariable Long configId) {
        return ResponseEntity.ok(screenConfigService.getConfigById(configId));
    }

    @Operation(summary = "Create new screen configuration")
    @PostMapping
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    public ResponseEntity<ScreenConfig> createConfig(@Valid @RequestBody ScreenConfigDto dto) {
        ScreenConfig created = screenConfigService.createConfig(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update screen configuration")
    @PutMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    public ResponseEntity<ScreenConfig> updateConfig(
            @PathVariable Long configId,
            @Valid @RequestBody ScreenConfigDto dto) {
        ScreenConfig updated = screenConfigService.updateConfig(configId, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Clone screen configuration")
    @PostMapping("/{configId}/clone")
    // DISABLED FOR TESTING: @PreAuthorize("hasAnyRole('ADMIN', 'CONFIG_EDITOR')")
    public ResponseEntity<ScreenConfig> cloneConfig(@PathVariable Long configId) {
        ScreenConfig cloned = screenConfigService.cloneConfig(configId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @Operation(summary = "Delete screen configuration")
    @DeleteMapping("/{configId}")
    // DISABLED FOR TESTING: @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        screenConfigService.deleteConfig(configId);
        return ResponseEntity.noContent().build();
    }
}

