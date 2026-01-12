package com.los.controller;

import com.los.dto.dashboard.DashboardFlowsResponse;
import com.los.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard API controller for home screen.
 * Provides list of available flows for users.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Dashboard API", description = "Dashboard endpoints for home screen and flow listing")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get all available flows for the given product/partner/branch.
     * Returns ACTIVE flows only, ready for UI rendering.
     * 
     * @param productCode Product code (required)
     * @param partnerCode Partner code (required)
     * @param branchCode Branch code (optional)
     * @return List of available flows with metadata
     */
    @Operation(
        summary = "Get available flows for dashboard",
        description = "Returns all ACTIVE flows for the given product/partner/branch combination. " +
                      "Used by Android home screen and Web dashboard to display flow tiles. " +
                      "If productCode/partnerCode not provided, returns ALL flows (for testing)."
    )
    @GetMapping("/flows")
    public ResponseEntity<DashboardFlowsResponse> getFlows(
            @Parameter(description = "Product code (optional - if not provided, returns all flows)")
            @RequestParam(required = false) String productCode,
            
            @Parameter(description = "Partner code (optional - if not provided, returns all flows)")
            @RequestParam(required = false) String partnerCode,
            
            @Parameter(description = "Branch code (optional)")
            @RequestParam(required = false) String branchCode
    ) {
        log.info("Dashboard API called with productCode={}, partnerCode={}, branchCode={}", 
                productCode, partnerCode, branchCode);

        DashboardFlowsResponse response = dashboardService.getAvailableFlows(
                productCode, 
                partnerCode, 
                branchCode
        );

        log.info("Returning {} flows for dashboard", response.getFlows().size());

        return ResponseEntity.ok(response);
    }
}
