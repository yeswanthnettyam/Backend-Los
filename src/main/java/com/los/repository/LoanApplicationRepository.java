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
}

