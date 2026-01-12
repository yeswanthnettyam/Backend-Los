package com.los.repository;

import com.los.config.entity.ValidationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValidationConfigRepository extends JpaRepository<ValidationConfig, Long> {
    
    @Query("""
        SELECT vc FROM ValidationConfig vc 
        WHERE vc.screenId = :screenId 
        AND vc.status = 'ACTIVE'
        AND (
            (vc.branchCode = :branchCode AND vc.partnerCode = :partnerCode AND vc.productCode = :productCode)
            OR (vc.branchCode IS NULL AND vc.partnerCode = :partnerCode AND vc.productCode = :productCode)
            OR (vc.branchCode IS NULL AND vc.partnerCode IS NULL AND vc.productCode = :productCode)
        )
        ORDER BY vc.branchCode DESC NULLS LAST, vc.partnerCode DESC NULLS LAST
        """)
    List<ValidationConfig> findByScope(
        @Param("screenId") String screenId,
        @Param("productCode") String productCode,
        @Param("partnerCode") String partnerCode,
        @Param("branchCode") String branchCode
    );
    
    Optional<ValidationConfig> findByConfigId(Long configId);
    
    List<ValidationConfig> findByScreenIdAndProductCodeAndPartnerCodeAndBranchCodeAndStatus(
        String screenId, String productCode, String partnerCode, String branchCode, String status
    );
    
    List<ValidationConfig> findByScreenIdAndStatus(String screenId, String status);
}

