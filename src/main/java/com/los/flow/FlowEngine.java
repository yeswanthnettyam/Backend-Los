package com.los.flow;

import com.los.config.entity.FlowConfig;
import com.los.config.entity.FlowSnapshot;
import com.los.config.entity.ScreenConfig;
import com.los.config.entity.ValidationConfig;
import com.los.config.entity.FieldMappingConfig;
import com.los.domain.LoanApplication;
import com.los.repository.FlowConfigRepository;
import com.los.repository.FlowSnapshotRepository;
import com.los.repository.ScreenConfigRepository;
import com.los.repository.ValidationConfigRepository;
import com.los.repository.FieldMappingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flow engine for navigation and decision logic.
 * Handles flow snapshots and next-screen determination.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowEngine {

    private final FlowConfigRepository flowConfigRepository;
    private final FlowSnapshotRepository flowSnapshotRepository;
    private final ScreenConfigRepository screenConfigRepository;
    private final ValidationConfigRepository validationConfigRepository;
    private final FieldMappingConfigRepository fieldMappingConfigRepository;

    /**
     * Get the next screen based on current screen and form data.
     * Creates snapshot on first screen submission.
     */
    @SuppressWarnings("unchecked")
    public String getNextScreen(LoanApplication application, String currentScreenId, Map<String, Object> formData) {
        
        // Get or create flow snapshot
        Map<String, Object> flowDefinition = getFlowDefinition(application);
        
        // Find current screen in flow
        Map<String, Object> screens = (Map<String, Object>) flowDefinition.get("screens");
        Map<String, Object> currentScreen = (Map<String, Object>) screens.get(currentScreenId);
        
        if (currentScreen == null) {
            log.error("Screen {} not found in flow", currentScreenId);
            throw new RuntimeException("Screen not found in flow");
        }
        
        // Evaluate next screen based on conditions
        return evaluateNextScreen(currentScreen, formData);
    }

    /**
     * Get flow definition, creating snapshot if needed.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getFlowDefinition(LoanApplication application) {
        
        // Check if snapshot exists
        if (application.getFlowSnapshotId() != null) {
            FlowSnapshot snapshot = flowSnapshotRepository.findById(application.getFlowSnapshotId())
                    .orElseThrow(() -> new RuntimeException("Flow snapshot not found"));
            return snapshot.getSnapshotData();
        }
        
        // No snapshot - this is the first screen submission
        // Create snapshot with current active configs
        return createFlowSnapshot(application);
    }

    /**
     * Create immutable snapshot of flow and screen configurations.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createFlowSnapshot(LoanApplication application) {
        log.info("Creating flow snapshot for application {}", application.getApplicationId());
        
        // Get active flow config
        List<FlowConfig> flowConfigs = flowConfigRepository.findByScope(
                "default", // For MVP, using default flow
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        
        if (flowConfigs.isEmpty()) {
            throw new RuntimeException("No active flow config found");
        }
        
        FlowConfig flowConfig = flowConfigs.get(0); // First match wins
        Map<String, Object> flowDefinition = new HashMap<>(flowConfig.getFlowDefinition());
        
        // Snapshot all screen configs referenced in flow
        Map<String, Object> screens = (Map<String, Object>) flowDefinition.get("screens");
        Map<String, Object> snapshotScreens = new HashMap<>();
        
        for (String screenId : screens.keySet()) {
            Map<String, Object> screenSnapshot = snapshotScreenConfig(
                    screenId,
                    application.getProductCode(),
                    application.getPartnerCode(),
                    application.getBranchCode()
            );
            snapshotScreens.put(screenId, screenSnapshot);
        }
        
        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("flowDefinition", flowDefinition);
        snapshotData.put("screenConfigs", snapshotScreens);
        
        // Save snapshot
        FlowSnapshot snapshot = FlowSnapshot.builder()
                .applicationId(application.getApplicationId())
                .flowConfigId(flowConfig.getConfigId())
                .snapshotData(snapshotData)
                .build();
        
        snapshot = flowSnapshotRepository.save(snapshot);
        
        // Update application with snapshot ID
        application.setFlowSnapshotId(snapshot.getSnapshotId());
        
        return flowDefinition;
    }

    /**
     * Snapshot screen configuration including validation and mapping.
     */
    private Map<String, Object> snapshotScreenConfig(String screenId, String productCode, String partnerCode, String branchCode) {
        Map<String, Object> snapshot = new HashMap<>();
        
        // Screen config
        List<ScreenConfig> screenConfigs = screenConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        if (!screenConfigs.isEmpty()) {
            snapshot.put("screenConfig", screenConfigs.get(0).getUiConfig());
        }
        
        // Validation config
        List<ValidationConfig> validationConfigs = validationConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        if (!validationConfigs.isEmpty()) {
            snapshot.put("validationConfig", validationConfigs.get(0).getValidationRules());
        }
        
        // Field mapping config
        List<FieldMappingConfig> mappingConfigs = fieldMappingConfigRepository.findByScope(screenId, productCode, partnerCode, branchCode);
        if (!mappingConfigs.isEmpty()) {
            snapshot.put("mappingConfig", mappingConfigs.get(0).getMappings());
        }
        
        return snapshot;
    }

    /**
     * Evaluate next screen based on conditions in flow.
     */
    @SuppressWarnings("unchecked")
    private String evaluateNextScreen(Map<String, Object> currentScreen, Map<String, Object> formData) {
        
        // Get next screen conditions
        Object nextObj = currentScreen.get("next");
        
        if (nextObj instanceof String) {
            // Simple next screen
            return (String) nextObj;
        }
        
        if (nextObj instanceof Map) {
            Map<String, Object> nextConfig = (Map<String, Object>) nextObj;
            
            // Check if there are conditions
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) nextConfig.get("conditions");
            
            if (conditions != null) {
                for (Map<String, Object> condition : conditions) {
                    if (evaluateCondition(condition, formData)) {
                        return (String) condition.get("screen");
                    }
                }
            }
            
            // Return default if no condition matches
            return (String) nextConfig.get("default");
        }
        
        // No next screen - end of flow
        return null;
    }

    /**
     * Evaluate a condition against form data.
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> formData) {
        String fieldId = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");
        
        Object actualValue = formData.get(fieldId);
        
        return switch (operator) {
            case "equals" -> Objects.equals(actualValue, expectedValue);
            case "notEquals" -> !Objects.equals(actualValue, expectedValue);
            case "greaterThan" -> compareValues(actualValue, expectedValue) > 0;
            case "lessThan" -> compareValues(actualValue, expectedValue) < 0;
            case "contains" -> actualValue != null && actualValue.toString().contains(expectedValue.toString());
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };
    }

    private int compareValues(Object actual, Object expected) {
        if (actual instanceof Number && expected instanceof Number) {
            double actualNum = ((Number) actual).doubleValue();
            double expectedNum = ((Number) expected).doubleValue();
            return Double.compare(actualNum, expectedNum);
        }
        return actual.toString().compareTo(expected.toString());
    }

    /**
     * Get screen configuration from snapshot or active config.
     */
    public Map<String, Object> getScreenConfig(LoanApplication application, String screenId) {
        
        // Try to get from snapshot first
        if (application.getFlowSnapshotId() != null) {
            return getScreenConfigFromSnapshot(application.getFlowSnapshotId(), screenId);
        }
        
        // Fall back to active config
        List<ScreenConfig> configs = screenConfigRepository.findByScope(
                screenId,
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        
        if (configs.isEmpty()) {
            throw new RuntimeException("Screen config not found: " + screenId);
        }
        
        return configs.get(0).getUiConfig();
    }

    /**
     * Get screen config from snapshot.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getScreenConfigFromSnapshot(Long snapshotId, String screenId) {
        FlowSnapshot snapshot = flowSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found"));
        
        Map<String, Object> snapshotData = snapshot.getSnapshotData();
        Map<String, Object> screenConfigs = (Map<String, Object>) snapshotData.get("screenConfigs");
        Map<String, Object> screenSnapshot = (Map<String, Object>) screenConfigs.get(screenId);
        
        if (screenSnapshot == null) {
            throw new RuntimeException("Screen not found in snapshot: " + screenId);
        }
        
        return (Map<String, Object>) screenSnapshot.get("screenConfig");
    }
}

