package com.los.config;

/**
 * Configuration status enumeration.
 * Represents the lifecycle state of configuration entities.
 * 
 * Lifecycle: DRAFT → ACTIVE → DEPRECATED
 */
public enum ConfigStatus {
    /**
     * Configuration is being developed and not ready for use.
     * - Can be edited freely
     * - NOT used at runtime
     * - Default status for new configs
     */
    DRAFT,
    
    /**
     * Configuration is approved and active in production.
     * - Used at runtime for NEW applications
     * - Only ONE active config allowed per scope
     * - Immutable once referenced in FlowSnapshot
     */
    ACTIVE,
    
    /**
     * Configuration was previously ACTIVE but has been superseded.
     * - Replaced by newer ACTIVE version
     * - Preserved for audit and existing FlowSnapshots
     * - NOT used for new applications
     */
    DEPRECATED,
    
    /**
     * Configuration is manually retired/disabled.
     * - Manually deactivated
     * - NOT used at runtime
     */
    INACTIVE;
    
    /**
     * Default status for new configurations.
     */
    public static final String DEFAULT_STATUS = "DRAFT";
    
    /**
     * Validate if a status string is valid.
     */
    public static boolean isValid(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        try {
            valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Get default status value.
     */
    public static String getDefault() {
        return DEFAULT_STATUS;
    }
    
    /**
     * Check if status is usable at runtime (ACTIVE only).
     */
    public static boolean isRuntimeUsable(String status) {
        return ACTIVE.name().equalsIgnoreCase(status);
    }
    
    /**
     * Check if status allows editing.
     */
    public static boolean isEditable(String status) {
        return DRAFT.name().equalsIgnoreCase(status);
    }
}
