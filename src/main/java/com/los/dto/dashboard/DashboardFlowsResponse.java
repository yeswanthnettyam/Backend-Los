package com.los.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for dashboard flows listing.
 * Contains all available flows for a given product/partner/branch combination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFlowsResponse {

    /**
     * List of available flows
     */
    private List<DashboardFlowResponse> flows;
}
