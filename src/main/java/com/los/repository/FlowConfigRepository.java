package com.los.repository;

import com.los.config.entity.FlowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlowConfigRepository extends JpaRepository<FlowConfig, Long> {
    
    @Query("""
        SELECT fc FROM FlowConfig fc 
        WHERE fc.flowId = :flowId 
        AND fc.status = 'ACTIVE'
        AND (
            (fc.branchCode = :branchCode AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
            OR (fc.branchCode IS NULL AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
            OR (fc.branchCode IS NULL AND fc.partnerCode IS NULL AND fc.productCode = :productCode)
        )
        ORDER BY fc.branchCode DESC NULLS LAST, fc.partnerCode DESC NULLS LAST
        """)
    List<FlowConfig> findByScope(
        @Param("flowId") String flowId,
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode
    );
    
    Optional<FlowConfig> findByConfigId(Long configId);
    
    /**
     * Find configs by exact scope and status.
     * Used for activation to find existing ACTIVE configs.
     */
    List<FlowConfig> findByFlowIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
        String flowId,
        String productCode,
        String partnerCode,
        String branchCode,
        String status
    );

    /**
     * Find all ACTIVE flows for dashboard.
     * Returns all flows matching the scope (product/partner/branch).
     * Uses scope resolution: branch > partner > product
     */
    @Query("""
        SELECT fc FROM FlowConfig fc 
        WHERE fc.status = 'ACTIVE'
        AND (
            (fc.branchCode = :branchCode AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
            OR (fc.branchCode IS NULL AND fc.partnerCode = :partnerCode AND fc.productCode = :productCode)
            OR (fc.branchCode IS NULL AND fc.partnerCode IS NULL AND fc.productCode = :productCode)
        )
        ORDER BY fc.flowId, fc.branchCode DESC NULLS LAST, fc.partnerCode DESC NULLS LAST
        """)
    List<FlowConfig> findAllActiveByScope(
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode
    );

    /**
     * Find all ACTIVE flows without scope filtering.
     * Used for testing when productCode/partnerCode not provided.
     */
    List<FlowConfig> findByStatus(String status);
}

