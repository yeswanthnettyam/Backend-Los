package com.los.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single flow on the dashboard.
 * Contains UI-ready metadata for rendering flow tiles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFlowResponse {

    /**
     * Unique flow identifier (used to start the flow)
     */
    private String flowId;

    /**
     * Display title for the flow tile
     */
    private String title;

    /**
     * Brief description of what the flow does
     */
    private String description;

    /**
     * Icon identifier for UI rendering
     */
    private String icon;

    /**
     * Product code this flow belongs to
     */
    private String productCode;

    /**
     * Partner code this flow belongs to
     */
    private String partnerCode;

    /**
     * Branch code (if applicable)
     */
    private String branchCode;

    /**
     * Current status (ACTIVE, DEPRECATED, etc.)
     */
    private String status;

    /**
     * Whether the flow can be started by the user
     */
    private Boolean startable;
}
