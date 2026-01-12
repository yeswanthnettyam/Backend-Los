package com.los.service;

import com.los.config.entity.ScreenConfig;
import com.los.dto.config.ScreenConfigDto;
import com.los.exception.ConfigNotFoundException;
import com.los.repository.ScreenConfigRepository;
import com.los.util.ConfigStatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing screen configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenConfigService {

    private final ScreenConfigRepository screenConfigRepository;

    public List<ScreenConfig> getAllConfigs() {
        return screenConfigRepository.findAll();
    }

    public ScreenConfig getConfigById(Long configId) {
        return screenConfigRepository.findById(configId)
                .orElseThrow(() -> new ConfigNotFoundException("Screen config not found: " + configId));
    }

    @Transactional
    public ScreenConfig createConfig(ScreenConfigDto dto) {
        // Set default status if not provided or validate if provided
        String status = ConfigStatusValidator.validateAndSetDefault(dto.getStatus());
        
        ScreenConfig config = ScreenConfig.builder()
                .screenId(dto.getScreenId())
                .productCode(dto.getProductCode())
                .partnerCode(dto.getPartnerCode())
                .branchCode(dto.getBranchCode())
                .version(1)
                .status(status)
                .uiConfig(dto.getUiConfig())
                .createdBy(dto.getCreatedBy())
                .build();

        log.debug("Creating screen config with status: {}", status);
        return screenConfigRepository.save(config);
    }

    @Transactional
    public ScreenConfig updateConfig(Long configId, ScreenConfigDto dto) {
        ScreenConfig config = getConfigById(configId);
        
        // Validate status if provided
        String status = ConfigStatusValidator.validateIfProvided(dto.getStatus());
        if (status != null) {
            config.setStatus(status);
        }
        
        config.setUiConfig(dto.getUiConfig());
        config.setUpdatedBy(dto.getUpdatedBy());

        log.debug("Updating screen config {} with status: {}", configId, config.getStatus());
        return screenConfigRepository.save(config);
    }

    @Transactional
    public ScreenConfig cloneConfig(Long configId) {
        ScreenConfig original = getConfigById(configId);
        
        ScreenConfig clone = ScreenConfig.builder()
                .screenId(original.getScreenId())
                .productCode(original.getProductCode())
                .partnerCode(original.getPartnerCode())
                .branchCode(original.getBranchCode())
                .version(1) // Reset version for clone
                .status("DRAFT")
                .uiConfig(original.getUiConfig())
                .createdBy(original.getCreatedBy())
                .build();

        return screenConfigRepository.save(clone);
    }

    @Transactional
    public void deleteConfig(Long configId) {
        if (!screenConfigRepository.existsById(configId)) {
            throw new ConfigNotFoundException("Screen config not found: " + configId);
        }
        screenConfigRepository.deleteById(configId);
    }
}

