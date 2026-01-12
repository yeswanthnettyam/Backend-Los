package com.los.repository;

import com.los.domain.VerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
    
    List<VerificationRecord> findByApplicationId(Long applicationId);
    
    Optional<VerificationRecord> findByApplicationIdAndFieldId(Long applicationId, String fieldId);
}

