package com.los.repository;

import com.los.config.entity.FieldMappingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldMappingConfigRepository extends JpaRepository<FieldMappingConfig, Long> {
    
    @Query("""
        SELECT fmc FROM FieldMappingConfig fmc 
        WHERE fmc.screenId = :screenId 
        AND fmc.status = 'ACTIVE'
        AND (
            (fmc.branchCode = :branchCode AND fmc.partnerCode = :partnerCode AND fmc.productCode = :productCode)
            OR (fmc.branchCode IS NULL AND fmc.partnerCode = :partnerCode AND fmc.productCode = :productCode)
            OR (fmc.branchCode IS NULL AND fmc.partnerCode IS NULL AND fmc.productCode = :productCode)
        )
        ORDER BY fmc.branchCode DESC NULLS LAST, fmc.partnerCode DESC NULLS LAST
        """)
    List<FieldMappingConfig> findByScope(
        @Param("screenId") String screenId,
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode
    );
    
    Optional<FieldMappingConfig> findByConfigId(Long configId);
    
    /**
     * Find configs by exact scope and status.
     * Used for activation to find existing ACTIVE configs.
     * Handles NULL values correctly.
     */
    @Query("""
        SELECT fmc FROM FieldMappingConfig fmc 
        WHERE fmc.screenId = :screenId 
        AND fmc.status = :status
        AND (fmc.productCode = :productCode OR (fmc.productCode IS NULL AND :productCode IS NULL))
        AND (fmc.partnerCode = :partnerCode OR (fmc.partnerCode IS NULL AND :partnerCode IS NULL))
        AND (fmc.branchCode = :branchCode OR (fmc.branchCode IS NULL AND :branchCode IS NULL))
        """)
    List<FieldMappingConfig> findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
        @Param("screenId") String screenId,
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode,
        @Param("status") String status
    );
}

