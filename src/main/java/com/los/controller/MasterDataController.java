package com.los.controller;

import com.los.dto.masterdata.MasterDataResponse;
import com.los.service.MasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Master Data API.
 * 
 * Provides a single consolidated endpoint to fetch all master data:
 * - Partners
 * - Products
 * - Branches
 * 
 * This API is used by:
 * - Config Builder (Web)
 * - Android App (Runtime & Dashboard)
 * 
 * Features:
 * - Returns only ACTIVE records
 * - Data is sorted by name (ascending)
 * - Returns empty arrays if no data (not 404)
 * - Results are cached for performance
 * - No authentication required (can be added later)
 */
@RestController
@RequestMapping("/api/v1/master-data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Master Data", description = "Consolidated Master Data API for Partners, Products, and Branches")
public class MasterDataController {

    private final MasterDataService masterDataService;

    /**
     * Get all master data in a single response.
     * 
     * Returns:
     * - All active partners (sorted by name)
     * - All active products (sorted by name)
     * - All active branches (sorted by name)
     * 
     * If no data exists, returns empty arrays (not 404).
     * Results are cached for performance.
     * 
     * @return consolidated master data response
     */
    @GetMapping
    @Operation(
        summary = "Get All Master Data",
        description = "Fetch all active partners, products, and branches in a single consolidated response. " +
                     "Returns empty arrays if no data found. Results are cached for performance."
    )
    public ResponseEntity<MasterDataResponse> getAllMasterData() {
        log.info("GET /api/v1/master-data - Fetching all master data");
        
        MasterDataResponse response = masterDataService.getAllMasterData();
        
        log.info("Returning {} partners, {} products, {} branches", 
                response.getPartners().size(), 
                response.getProducts().size(), 
                response.getBranches().size());
        
        return ResponseEntity.ok(response);
    }
}
