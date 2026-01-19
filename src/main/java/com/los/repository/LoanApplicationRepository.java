package com.los.repository;

import com.los.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    
    List<LoanApplication> findByProductCodeAndPartnerCode(String productCode, String partnerCode);
    
    Optional<LoanApplication> findByApplicationIdAndStatus(Long applicationId, String status);
    
    /**
     * Find the most recent application by scope and current screen.
     * Used when applicationId is null (e.g., first screen submission after flow start).
     */
    Optional<LoanApplication> findFirstByProductCodeAndPartnerCodeAndBranchCodeAndCurrentScreenIdOrderByCreatedAtDesc(
            String productCode, String partnerCode, String branchCode, String currentScreenId);
    
    /**
     * Find the most recent application by scope (without branch).
     * Used when applicationId is null and branchCode is also null.
     */
    Optional<LoanApplication> findFirstByProductCodeAndPartnerCodeAndCurrentScreenIdOrderByCreatedAtDesc(
            String productCode, String partnerCode, String currentScreenId);
}

