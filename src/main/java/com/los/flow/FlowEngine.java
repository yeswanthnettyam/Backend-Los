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
import com.los.service.ConfigResolutionService;
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
    private final ConfigResolutionService configResolutionService;

    /**
     * Get the start screen for a flow and create snapshot.
     * Called on flow start (currentScreenId == null).
     * 
     * @param application The loan application (must have productCode, partnerCode, branchCode)
     * @param flowId The flow ID to start
     * @return The start screen ID from the flow definition
     */
    @SuppressWarnings("unchecked")
    public String getStartScreen(LoanApplication application, String flowId) {
        log.info("Getting start screen for flowId={}, application={}", flowId, application.getApplicationId());

        // Resolve active flow config
        FlowConfig flowConfig = configResolutionService.resolveActiveFlowConfig(
                flowId,
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );

        Map<String, Object> flowDefinition = flowConfig.getFlowDefinition();
        
        // Extract start screen from flow definition
        String startScreenId = (String) flowDefinition.get("startScreen");
        
        if (startScreenId == null || startScreenId.isEmpty()) {
            throw new RuntimeException("Flow definition does not specify a startScreen");
        }

        log.info("Start screen for flowId={} is: {}", flowId, startScreenId);

        // Create flow snapshot (stores flowId, version, screen configs)
        createFlowSnapshotForStart(application, flowConfig);

        return startScreenId;
    }

    /**
     * Create flow snapshot on flow start.
     * Stores immutable copy of flow definition and all screen configs.
     */
    @SuppressWarnings("unchecked")
    private void createFlowSnapshotForStart(LoanApplication application, FlowConfig flowConfig) {
        log.info("Creating flow snapshot for application {} on flow start", application.getApplicationId());

        Map<String, Object> flowDefinition = new HashMap<>(flowConfig.getFlowDefinition());

        // Get start screen ID - it must be included in snapshot
        String startScreenId = (String) flowDefinition.get("startScreen");
        
        // Snapshot all screen configs referenced in flow
        // Handle both Map and List formats for screens
        Object screensObj = flowDefinition.get("screens");
        Map<String, Object> snapshotScreens = new HashMap<>();
        
        // Always include the start screen in snapshot
        Set<String> screenIdsToSnapshot = new HashSet<>();
        if (startScreenId != null && !startScreenId.isEmpty()) {
            screenIdsToSnapshot.add(startScreenId);
        }

        if (screensObj != null) {
            Set<String> screenIds = extractScreenIds(screensObj);
            screenIdsToSnapshot.addAll(screenIds);
        }
        
        // Snapshot all screens (including start screen)
        for (String screenId : screenIdsToSnapshot) {
            Map<String, Object> screenSnapshot = snapshotScreenConfig(
                    screenId,
                    application.getProductCode(),
                    application.getPartnerCode(),
                    application.getBranchCode()
            );
            snapshotScreens.put(screenId, screenSnapshot);
        }

        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("flowId", flowConfig.getFlowId());
        snapshotData.put("flowVersion", flowConfig.getVersion());
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

        log.info("Flow snapshot created with ID={} for application={}", 
                snapshot.getSnapshotId(), application.getApplicationId());
    }

    /**
     * Get the next screen based on current screen and form data.
     * Creates snapshot on first screen submission.
     */
    @SuppressWarnings("unchecked")
    public String getNextScreen(LoanApplication application, String currentScreenId, Map<String, Object> formData) {
        
        // Get or create flow snapshot
        Map<String, Object> flowDefinition = getFlowDefinition(application);
        
        // Find current screen in flow
        Object screensObj = flowDefinition.get("screens");
        Map<String, Object> currentScreen = findScreenInFlow(screensObj, currentScreenId);
        
        if (currentScreen == null) {
            log.error("Screen {} not found in flow", currentScreenId);
            throw new RuntimeException("Screen not found in flow: " + currentScreenId);
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
     * This is called during screen progression if snapshot doesn't exist yet (legacy path).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createFlowSnapshot(LoanApplication application) {
        log.info("Creating flow snapshot for application {} (legacy path)", application.getApplicationId());
        
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
        Object screensObj = flowDefinition.get("screens");
        Map<String, Object> snapshotScreens = new HashMap<>();
        
        if (screensObj != null) {
            Set<String> screenIds = extractScreenIds(screensObj);
            
            for (String screenId : screenIds) {
                Map<String, Object> screenSnapshot = snapshotScreenConfig(
                        screenId,
                        application.getProductCode(),
                        application.getPartnerCode(),
                        application.getBranchCode()
                );
                snapshotScreens.put(screenId, screenSnapshot);
            }
        }
        
        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("flowId", flowConfig.getFlowId());
        snapshotData.put("flowVersion", flowConfig.getVersion());
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
     * Extract screen IDs from screens object (handles both Map and List formats).
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractScreenIds(Object screensObj) {
        Set<String> screenIds = new HashSet<>();
        
        if (screensObj instanceof Map) {
            // Format: {"screen1": {...}, "screen2": {...}}
            Map<String, Object> screens = (Map<String, Object>) screensObj;
            screenIds.addAll(screens.keySet());
        } else if (screensObj instanceof List) {
            // Format: [{"id": "screen1", ...}, {"id": "screen2", ...}]
            List<Object> screens = (List<Object>) screensObj;
            for (Object screenObj : screens) {
                if (screenObj instanceof Map) {
                    Map<String, Object> screen = (Map<String, Object>) screenObj;
                    String screenId = (String) screen.get("id");
                    if (screenId != null) {
                        screenIds.add(screenId);
                    }
                } else if (screenObj instanceof String) {
                    // Format: ["screen1", "screen2"]
                    screenIds.add((String) screenObj);
                }
            }
        }
        
        return screenIds;
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
     * Find a screen in the flow definition (handles both Map and List formats).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findScreenInFlow(Object screensObj, String screenId) {
        if (screensObj == null) {
            return null;
        }
        
        if (screensObj instanceof Map) {
            // Format: {"screen1": {...}, "screen2": {...}}
            Map<String, Object> screens = (Map<String, Object>) screensObj;
            return (Map<String, Object>) screens.get(screenId);
        } else if (screensObj instanceof List) {
            // Format: [{"id": "screen1", ...}, {"id": "screen2", ...}]
            List<Object> screens = (List<Object>) screensObj;
            for (Object screenObj : screens) {
                if (screenObj instanceof Map) {
                    Map<String, Object> screen = (Map<String, Object>) screenObj;
                    String id = (String) screen.get("id");
                    if (screenId.equals(id)) {
                        return screen;
                    }
                }
            }
        }
        
        return null;
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

