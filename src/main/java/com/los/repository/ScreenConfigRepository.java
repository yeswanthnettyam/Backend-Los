package com.los.repository;

import com.los.config.entity.ScreenConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenConfigRepository extends JpaRepository<ScreenConfig, Long> {
    
    List<ScreenConfig> findByScreenIdAndStatus(String screenId, String status);
    
    @Query("""
        SELECT sc FROM ScreenConfig sc 
        WHERE sc.screenId = :screenId 
        AND sc.status = 'ACTIVE'
        AND (
            (sc.branchCode = :branchCode AND sc.partnerCode = :partnerCode AND sc.productCode = :productCode)
            OR (sc.branchCode IS NULL AND sc.partnerCode = :partnerCode AND sc.productCode = :productCode)
            OR (sc.branchCode IS NULL AND sc.partnerCode IS NULL AND sc.productCode = :productCode)
        )
        ORDER BY sc.branchCode DESC NULLS LAST, sc.partnerCode DESC NULLS LAST
        """)
    List<ScreenConfig> findByScope(
        @Param("screenId") String screenId,
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode
    );
    
    Optional<ScreenConfig> findByConfigId(Long configId);
    
    /**
     * Find configs by exact scope and status.
     * Used for activation to find existing ACTIVE configs.
     */
    List<ScreenConfig> findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
        String screenId,
        String productCode,
        String partnerCode,
        String branchCode,
        String status
    );
}

