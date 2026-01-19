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
            log.debug("Extracted {} screen IDs from flow definition: {}", screenIds.size(), screenIds);
        }
        
        log.info("Snapshoting {} screens for application {}: {}", 
                screenIdsToSnapshot.size(), application.getApplicationId(), screenIdsToSnapshot);
        
        // Snapshot all screens (including start screen)
        for (String screenId : screenIdsToSnapshot) {
            try {
                Map<String, Object> screenSnapshot = snapshotScreenConfig(
                        screenId,
                        application.getProductCode(),
                        application.getPartnerCode(),
                        application.getBranchCode()
                );
                if (screenSnapshot != null && !screenSnapshot.isEmpty()) {
                    snapshotScreens.put(screenId, screenSnapshot);
                    log.debug("Successfully snapshotted screen config for: {}", screenId);
                } else {
                    log.warn("Screen snapshot is null or empty for screenId: {}", screenId);
                }
            } catch (Exception e) {
                log.error("Failed to snapshot screen config for screenId: {}. Error: {}", screenId, e.getMessage(), e);
                // Continue with other screens - don't fail entire snapshot
            }
        }
        
        log.info("Successfully snapshotted {} screen configs for application {}", 
                snapshotScreens.size(), application.getApplicationId());

        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("flowId", flowConfig.getFlowId());
        snapshotData.put("flowVersion", flowConfig.getVersion());
        snapshotData.put("flowDefinition", flowDefinition);
        snapshotData.put("screenConfigs", snapshotScreens);

        // Log flow definition structure for debugging
        Object screensInFlowDef = flowDefinition.get("screens");
        log.debug("Creating snapshot - flowDefinition has screens: type={}, count={}", 
                screensInFlowDef != null ? screensInFlowDef.getClass().getSimpleName() : "null",
                screensInFlowDef instanceof List ? ((List<?>) screensInFlowDef).size() : 
                screensInFlowDef instanceof Map ? ((Map<?, ?>) screensInFlowDef).size() : 0);

        // Save snapshot
        FlowSnapshot snapshot = FlowSnapshot.builder()
                .applicationId(application.getApplicationId())
                .flowConfigId(flowConfig.getConfigId())
                .snapshotData(snapshotData)
                .build();

        snapshot = flowSnapshotRepository.save(snapshot);

        // Update application with snapshot ID
        application.setFlowSnapshotId(snapshot.getSnapshotId());

        log.info("Flow snapshot created with ID={} for application={}, flowId={}, screens count={}", 
                snapshot.getSnapshotId(), application.getApplicationId(), flowConfig.getFlowId(),
                screensInFlowDef instanceof List ? ((List<?>) screensInFlowDef).size() : 
                screensInFlowDef instanceof Map ? ((Map<?, ?>) screensInFlowDef).size() : 0);
    }

    /**
     * Get the next screen based on current screen and form data.
     * Creates snapshot on first screen submission.
     * 
     * @param application The loan application
     * @param currentScreenId The current screen ID
     * @param formData The form data submitted
     * @param flowId The flow ID (used if snapshot doesn't exist yet)
     */
    @SuppressWarnings("unchecked")
    public String getNextScreen(LoanApplication application, String currentScreenId, Map<String, Object> formData, String flowId) {
        
        // Get or create flow snapshot
        Map<String, Object> flowDefinition = getFlowDefinition(application, flowId);
        
        // Find current screen in flow
        Object screensObj = flowDefinition.get("screens");
        log.debug("Looking for screen {} in flow. Screens object type: {}, value: {}", 
                currentScreenId, screensObj != null ? screensObj.getClass().getSimpleName() : "null", screensObj);
        Map<String, Object> currentScreen = findScreenInFlow(screensObj, currentScreenId);
        
        if (currentScreen == null) {
            // Log available screen IDs for debugging
            StringBuilder availableScreens = new StringBuilder();
            if (screensObj instanceof List) {
                List<?> screens = (List<?>) screensObj;
                for (Object screen : screens) {
                    if (screen instanceof Map) {
                        Map<?, ?> screenMap = (Map<?, ?>) screen;
                        String id = (String) screenMap.get("screenId");
                        if (id == null) {
                            id = (String) screenMap.get("id");
                        }
                        if (id != null) {
                            if (availableScreens.length() > 0) {
                                availableScreens.append(", ");
                            }
                            availableScreens.append(id);
                        }
                    }
                }
            } else if (screensObj instanceof Map) {
                availableScreens.append(((Map<?, ?>) screensObj).keySet());
            }
            log.error("Screen {} not found in flow. Available screens: [{}]", currentScreenId, availableScreens);
            throw new RuntimeException("Screen not found in flow: " + currentScreenId);
        }
        
        // Evaluate next screen based on conditions
        log.debug("Evaluating next screen for currentScreenId={}, formData keys: {}", 
                currentScreenId, formData != null ? formData.keySet() : "null");
        String nextScreen = evaluateNextScreen(currentScreen, formData);
        log.info("Next screen determined: {} (from currentScreen: {})", nextScreen, currentScreenId);
        return nextScreen;
    }

    /**
     * Get flow definition, creating snapshot if needed.
     * 
     * @param application The loan application
     * @param flowId The flow ID (required if snapshot doesn't exist)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getFlowDefinition(LoanApplication application, String flowId) {
        log.debug("Getting flow definition for application ID={}, snapshotId={}, flowId={}", 
                application.getApplicationId(), application.getFlowSnapshotId(), flowId);
        
        // Check if snapshot exists
        if (application.getFlowSnapshotId() != null) {
            log.debug("Application {} has snapshot ID={}, retrieving from snapshot", 
                    application.getApplicationId(), application.getFlowSnapshotId());
            FlowSnapshot snapshot = flowSnapshotRepository.findById(application.getFlowSnapshotId())
                    .orElseThrow(() -> new RuntimeException("Flow snapshot not found: " + application.getFlowSnapshotId()));
            Map<String, Object> snapshotData = snapshot.getSnapshotData();
            log.debug("Retrieved snapshot data, keys: {}", snapshotData.keySet());
            
            // Extract flowDefinition from snapshotData (snapshotData contains flowDefinition, screenConfigs, flowId, flowVersion)
            Map<String, Object> flowDefinition = (Map<String, Object>) snapshotData.get("flowDefinition");
            if (flowDefinition == null) {
                log.error("Flow definition not found in snapshot data for application {}. Snapshot keys: {}", 
                        application.getApplicationId(), snapshotData.keySet());
                throw new RuntimeException("Flow definition not found in snapshot");
            }
            Object screensInFlowDef = flowDefinition.get("screens");
            log.info("Retrieved flow definition from snapshot ID={} for application={}. Screens: type={}, count={}", 
                    snapshot.getSnapshotId(), application.getApplicationId(),
                    screensInFlowDef != null ? screensInFlowDef.getClass().getSimpleName() : "null",
                    screensInFlowDef instanceof List ? ((List<?>) screensInFlowDef).size() : 
                    screensInFlowDef instanceof Map ? ((Map<?, ?>) screensInFlowDef).size() : 0);
            return flowDefinition;
        }
        
        // No snapshot - this is the first screen submission
        // Create snapshot with current active configs
        if (flowId == null || flowId.isBlank()) {
            throw new IllegalArgumentException("flowId is required when creating snapshot for application " + application.getApplicationId());
        }
        log.info("Application {} has no snapshot, creating new snapshot with flowId={}", application.getApplicationId(), flowId);
        return createFlowSnapshot(application, flowId);
    }

    /**
     * Create immutable snapshot of flow and screen configurations.
     * This is called during screen progression if snapshot doesn't exist yet (legacy path).
     * 
     * @param application The loan application
     * @param flowId The flow ID to create snapshot for
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> createFlowSnapshot(LoanApplication application, String flowId) {
        log.info("Creating flow snapshot for application {} (legacy path) with flowId={}", application.getApplicationId(), flowId);
        
        // Get active flow config using the provided flowId
        List<FlowConfig> flowConfigs = flowConfigRepository.findByScope(
                flowId,
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
            // Format: [{"id": "screen1", ...}, {"screenId": "screen1", ...}, {"id": "screen2", ...}]
            List<Object> screens = (List<Object>) screensObj;
            for (Object screenObj : screens) {
                if (screenObj instanceof Map) {
                    Map<String, Object> screen = (Map<String, Object>) screenObj;
                    // Check both "id" and "screenId" fields for compatibility
                    String screenId = (String) screen.get("id");
                    if (screenId == null) {
                        screenId = (String) screen.get("screenId");
                    }
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
        
        // Screen config - use ConfigResolutionService to ensure we only get ACTIVE configs
        try {
            Map<String, Object> screenConfig = configResolutionService.getScreenConfig(screenId, productCode, partnerCode, branchCode);
            if (screenConfig != null && !screenConfig.isEmpty()) {
                snapshot.put("screenConfig", screenConfig);
            } else {
                log.warn("Screen config is null or empty for screenId: {} (productCode={}, partnerCode={}, branchCode={})", 
                        screenId, productCode, partnerCode, branchCode);
            }
        } catch (Exception e) {
            log.error("Failed to get screen config for screenId: {} (productCode={}, partnerCode={}, branchCode={}). Error: {}", 
                    screenId, productCode, partnerCode, branchCode, e.getMessage());
            // Don't throw - allow snapshot to continue with other screens
        }
        
        // Validation config (only include ACTIVE configs)
        try {
            Map<String, Object> validationConfig = configResolutionService.getValidationConfig(screenId, productCode, partnerCode, branchCode);
            if (validationConfig != null && !validationConfig.isEmpty()) {
                snapshot.put("validationConfig", validationConfig);
            }
        } catch (Exception e) {
            log.debug("No validation config found for screenId: {} (will be skipped). Error: {}", screenId, e.getMessage());
        }
        
        // Field mapping config
        try {
            Map<String, Object> mappingConfig = configResolutionService.getFieldMappingConfig(screenId, productCode, partnerCode, branchCode);
            if (mappingConfig != null && !mappingConfig.isEmpty()) {
                snapshot.put("mappingConfig", mappingConfig);
            }
        } catch (Exception e) {
            log.debug("No field mapping config found for screenId: {} (will be skipped). Error: {}", screenId, e.getMessage());
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
            // Format: [{"id": "screen1", ...}, {"screenId": "screen1", ...}, {"id": "screen2", ...}]
            List<Object> screens = (List<Object>) screensObj;
            for (Object screenObj : screens) {
                if (screenObj instanceof Map) {
                    Map<String, Object> screen = (Map<String, Object>) screenObj;
                    // Check both "id" and "screenId" fields for compatibility
                    String id = (String) screen.get("id");
                    String screenIdField = (String) screen.get("screenId");
                    if (screenId.equals(id) || screenId.equals(screenIdField)) {
                        return screen;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Evaluate next screen based on conditions in flow.
     * Supports both "next" and "defaultNext" fields for compatibility.
     */
    @SuppressWarnings("unchecked")
    private String evaluateNextScreen(Map<String, Object> currentScreen, Map<String, Object> formData) {
        log.debug("Evaluating next screen. Current screen keys: {}, formData: {}", 
                currentScreen.keySet(), formData);
        
        // Check for conditions array first (if present, evaluate them)
        Object conditionsObj = currentScreen.get("conditions");
        log.debug("Conditions object: {}", conditionsObj);
        if (conditionsObj instanceof List) {
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionsObj;
            for (Map<String, Object> condition : conditions) {
                // Support both formats:
                // Format 1: {"if": {...}, "then": {...}}
                // Format 2: {"field": "...", "operator": "...", "value": "..."}
                Map<String, Object> ifCondition = (Map<String, Object>) condition.get("if");
                if (ifCondition != null) {
                    // Format 1: Extract condition from "if" block
                    boolean conditionMatched = evaluateConditionFromIf(ifCondition, formData);
                    log.debug("Condition evaluation result: {} (ifCondition: {}, formData: {})", 
                            conditionMatched, ifCondition, formData);
                    if (conditionMatched) {
                        // Condition matched - get the nextScreen from "then" block
                        Object thenObj = condition.get("then");
                        if (thenObj instanceof Map) {
                            Map<String, Object> then = (Map<String, Object>) thenObj;
                            String nextScreen = (String) then.get("nextScreen");
                            log.debug("Condition matched. nextScreen from 'then': '{}'", nextScreen);
                            if (nextScreen != null && !nextScreen.isEmpty() && !"__FLOW_END__".equals(nextScreen)) {
                                log.info("Returning nextScreen from condition: {}", nextScreen);
                                return nextScreen;
                            } else if ("__FLOW_END__".equals(nextScreen)) {
                                log.info("Flow end marker in condition, returning null");
                                return null; // End of flow
                            } else if (nextScreen != null && nextScreen.isEmpty()) {
                                log.warn("Empty nextScreen in condition 'then' block - falling back to defaultNext");
                                // Empty string - fall through to defaultNext
                            }
                        }
                    }
                } else {
                    // Format 2: Direct condition format
                    if (evaluateCondition(condition, formData)) {
                        String nextScreen = (String) condition.get("screen");
                        if (nextScreen != null && !nextScreen.isEmpty() && !"__FLOW_END__".equals(nextScreen)) {
                            return nextScreen;
                        } else if ("__FLOW_END__".equals(nextScreen)) {
                            return null; // End of flow
                        }
                    }
                }
            }
        }
        
        // No conditions matched or no conditions - use defaultNext
        // Support both "next" and "defaultNext" field names
        Object nextObj = currentScreen.get("next");
        if (nextObj == null) {
            nextObj = currentScreen.get("defaultNext");
            log.debug("Using defaultNext field (next was null). defaultNext: {}", nextObj);
        } else {
            log.debug("Using next field: {}", nextObj);
        }
        
        if (nextObj instanceof String) {
            // Simple next screen
            String nextScreen = (String) nextObj;
            log.debug("Next screen from defaultNext/next field: {}", nextScreen);
            // Handle special end-of-flow marker
            if ("__FLOW_END__".equals(nextScreen)) {
                log.info("Flow end marker detected, returning null");
                return null;
            }
            return nextScreen;
        }
        
        if (nextObj instanceof Map) {
            Map<String, Object> nextConfig = (Map<String, Object>) nextObj;
            
            // Check if there are conditions in next config
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) nextConfig.get("conditions");
            
            if (conditions != null) {
                for (Map<String, Object> condition : conditions) {
                    if (evaluateCondition(condition, formData)) {
                        return (String) condition.get("screen");
                    }
                }
            }
            
            // Return default if no condition matches
            String defaultNext = (String) nextConfig.get("default");
            if (defaultNext == null) {
                defaultNext = (String) nextConfig.get("defaultNext");
            }
            if ("__FLOW_END__".equals(defaultNext)) {
                return null;
            }
            return defaultNext;
        }
        
        // No next screen - end of flow
        return null;
    }

    /**
     * Evaluate a condition against form data (direct format: field, operator, value).
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateCondition(Map<String, Object> condition, Map<String, Object> formData) {
        String fieldId = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");
        
        Object actualValue = formData.get(fieldId);
        
        return evaluateConditionLogic(operator, actualValue, expectedValue);
    }
    
    /**
     * Evaluate a condition from "if" block format.
     * Supports: {"source": "FORM_DATA", "operator": "EQUALS", "value": ""}
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateConditionFromIf(Map<String, Object> ifCondition, Map<String, Object> formData) {
        String source = (String) ifCondition.get("source");
        String operator = (String) ifCondition.get("operator");
        Object expectedValue = ifCondition.get("value");
        
        // Currently only supports FORM_DATA source
        if (!"FORM_DATA".equals(source)) {
            log.warn("Unsupported condition source: {}", source);
            return false;
        }
        
        // For FORM_DATA, we need a fieldId - check if it's provided
        String fieldId = (String) ifCondition.get("fieldId");
        if (fieldId == null) {
            // If no fieldId, might be checking if formData is empty
            if ("EQUALS".equalsIgnoreCase(operator) && "".equals(expectedValue)) {
                return formData == null || formData.isEmpty();
            }
            log.warn("Condition from 'if' block missing fieldId");
            return false;
        }
        
        Object actualValue = formData.get(fieldId);
        return evaluateConditionLogic(operator, actualValue, expectedValue);
    }
    
    /**
     * Core condition evaluation logic.
     */
    private boolean evaluateConditionLogic(String operator, Object actualValue, Object expectedValue) {
        // Normalize operator to lowercase for comparison
        String op = operator != null ? operator.toLowerCase() : "";
        
        return switch (op) {
            case "equals", "==" -> Objects.equals(actualValue, expectedValue);
            case "notequals", "!=" -> !Objects.equals(actualValue, expectedValue);
            case "greaterthan", ">" -> compareValues(actualValue, expectedValue) > 0;
            case "lessthan", "<" -> compareValues(actualValue, expectedValue) < 0;
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
            try {
                return getScreenConfigFromSnapshot(application.getFlowSnapshotId(), screenId);
            } catch (RuntimeException e) {
                // Screen not found in snapshot - fallback to active config
                log.warn("Screen {} not found in snapshot ID={} for application {}. Falling back to active config. Error: {}", 
                        screenId, application.getFlowSnapshotId(), application.getApplicationId(), e.getMessage());
                // Continue to fallback logic below
            }
        }
        
        // Fall back to active config
        log.debug("Getting screen config for screenId={} from active configs (productCode={}, partnerCode={}, branchCode={})", 
                screenId, application.getProductCode(), application.getPartnerCode(), application.getBranchCode());
        List<ScreenConfig> configs = screenConfigRepository.findByScope(
                screenId,
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        
        if (configs.isEmpty()) {
            throw new RuntimeException("Screen config not found: " + screenId + 
                    " (productCode=" + application.getProductCode() + 
                    ", partnerCode=" + application.getPartnerCode() + 
                    ", branchCode=" + application.getBranchCode() + ")");
        }
        
        return configs.get(0).getUiConfig();
    }

    /**
     * Get screen config from snapshot.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getScreenConfigFromSnapshot(Long snapshotId, String screenId) {
        FlowSnapshot snapshot = flowSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));
        
        Map<String, Object> snapshotData = snapshot.getSnapshotData();
        Map<String, Object> screenConfigs = (Map<String, Object>) snapshotData.get("screenConfigs");
        
        if (screenConfigs == null) {
            log.error("screenConfigs is null in snapshot ID={}", snapshotId);
            throw new RuntimeException("Screen configs not found in snapshot: " + snapshotId);
        }
        
        Map<String, Object> screenSnapshot = (Map<String, Object>) screenConfigs.get(screenId);
        
        if (screenSnapshot == null) {
            log.error("Screen {} not found in snapshot ID={}. Available screens in snapshot: {}", 
                    screenId, snapshotId, screenConfigs.keySet());
            throw new RuntimeException("Screen not found in snapshot: " + screenId + 
                    ". Available screens: " + screenConfigs.keySet());
        }
        
        Map<String, Object> screenConfig = (Map<String, Object>) screenSnapshot.get("screenConfig");
        if (screenConfig == null) {
            log.error("screenConfig is null for screen {} in snapshot ID={}. Screen snapshot keys: {}", 
                    screenId, snapshotId, screenSnapshot.keySet());
            throw new RuntimeException("Screen config data not found in snapshot for screen: " + screenId);
        }
        
        return screenConfig;
    }
}

