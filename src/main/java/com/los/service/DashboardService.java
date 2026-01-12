package com.los.service;

import com.los.config.entity.FlowConfig;
import com.los.dto.dashboard.DashboardFlowResponse;
import com.los.dto.dashboard.DashboardFlowsResponse;
import com.los.repository.FlowConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for dashboard operations.
 * Provides list of available flows for the home screen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FlowConfigRepository flowConfigRepository;

    /**
     * Get all available flows for a given product/partner/branch.
     * Returns ACTIVE flows only, applying scope resolution.
     * If productCode/partnerCode are null, returns ALL ACTIVE flows (for testing).
     * 
     * @param productCode Product code (optional - if null, returns all flows)
     * @param partnerCode Partner code (optional - if null, returns all flows)
     * @param branchCode Branch code (optional)
     * @return Dashboard response with list of flows
     */
    public DashboardFlowsResponse getAvailableFlows(String productCode, String partnerCode, String branchCode) {
        log.info("Fetching available flows for product={}, partner={}, branch={}", 
                productCode, partnerCode, branchCode);

        List<FlowConfig> flowConfigs;

        // If no product/partner provided, return ALL ACTIVE flows (for testing)
        if (productCode == null || partnerCode == null) {
            log.info("No product/partner provided - returning ALL ACTIVE flows (testing mode)");
            flowConfigs = flowConfigRepository.findByStatus("ACTIVE");
        } else {
            // Fetch all ACTIVE flows matching the scope
            flowConfigs = flowConfigRepository.findAllActiveByScope(
                    productCode, 
                    partnerCode, 
                    branchCode
            );
        }

        log.debug("Found {} flow configs before deduplication", flowConfigs.size());

        // Apply scope resolution: for each flowId, take the most specific config
        Map<String, FlowConfig> deduplicatedFlows = deduplicateByScope(flowConfigs);

        log.info("Returning {} unique flows after scope resolution", deduplicatedFlows.size());

        // Map to DTOs
        List<DashboardFlowResponse> flowResponses = deduplicatedFlows.values().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return DashboardFlowsResponse.builder()
                .flows(flowResponses)
                .build();
    }

    /**
     * Deduplicate flows by flowId, keeping the most specific scope.
     * Precedence: BRANCH > PARTNER > PRODUCT
     */
    private Map<String, FlowConfig> deduplicateByScope(List<FlowConfig> configs) {
        Map<String, FlowConfig> result = new HashMap<>();

        for (FlowConfig config : configs) {
            String flowId = config.getFlowId();
            
            if (!result.containsKey(flowId)) {
                // First occurrence of this flowId
                result.put(flowId, config);
            } else {
                // Already have a config for this flowId
                // Keep the more specific one
                FlowConfig existing = result.get(flowId);
                if (isMoreSpecific(config, existing)) {
                    result.put(flowId, config);
                }
            }
        }

        return result;
    }

    /**
     * Check if config1 is more specific than config2.
     * Branch-level is most specific, then partner, then product.
     */
    private boolean isMoreSpecific(FlowConfig config1, FlowConfig config2) {
        // Branch level is most specific
        if (config1.getBranchCode() != null && config2.getBranchCode() == null) {
            return true;
        }
        if (config1.getBranchCode() == null && config2.getBranchCode() != null) {
            return false;
        }
        
        // Partner level
        if (config1.getPartnerCode() != null && config2.getPartnerCode() == null) {
            return true;
        }
        if (config1.getPartnerCode() == null && config2.getPartnerCode() != null) {
            return false;
        }
        
        // Same specificity level
        return false;
    }

    /**
     * Map FlowConfig entity to DashboardFlowResponse DTO.
     */
    @SuppressWarnings("unchecked")
    private DashboardFlowResponse mapToDto(FlowConfig config) {
        Map<String, Object> dashboardMeta = config.getDashboardMeta();
        
        // Extract dashboard metadata with defaults
        String title = dashboardMeta != null && dashboardMeta.containsKey("title") 
                ? (String) dashboardMeta.get("title") 
                : config.getFlowId();
        
        String description = dashboardMeta != null && dashboardMeta.containsKey("description") 
                ? (String) dashboardMeta.get("description") 
                : "Flow description not available";
        
        String icon = dashboardMeta != null && dashboardMeta.containsKey("icon") 
                ? (String) dashboardMeta.get("icon") 
                : "DEFAULT";

        return DashboardFlowResponse.builder()
                .flowId(config.getFlowId())
                .title(title)
                .description(description)
                .icon(icon)
                .productCode(config.getProductCode())
                .partnerCode(config.getPartnerCode())
                .branchCode(config.getBranchCode())
                .status(config.getStatus())
                .startable(true) // All ACTIVE flows are startable
                .build();
    }
}
