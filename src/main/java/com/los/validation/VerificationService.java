package com.los.validation;

import com.los.domain.VerificationRecord;
import com.los.repository.VerificationRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling field verification (OTP, PAN, Aadhaar, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRecordRepository verificationRecordRepository;

    /**
     * Check if a field has been verified for an application.
     */
    public boolean isVerified(Long applicationId, String fieldId) {
        Optional<VerificationRecord> record = verificationRecordRepository
                .findByApplicationIdAndFieldId(applicationId, fieldId);
        
        return record.isPresent() && "SUCCESS".equals(record.get().getStatus());
    }

    /**
     * Record a verification attempt.
     * In production, this would call external services (OTP provider, PAN verification, etc.)
     */
    public VerificationResult verify(Long applicationId, String fieldId, String verificationType, Object fieldValue) {
        log.info("Verifying field {} for application {}", fieldId, applicationId);

        // Get or create verification record
        VerificationRecord record = verificationRecordRepository
                .findByApplicationIdAndFieldId(applicationId, fieldId)
                .orElse(VerificationRecord.builder()
                        .applicationId(applicationId)
                        .fieldId(fieldId)
                        .verificationType(verificationType)
                        .attemptCount(0)
                        .build());

        // Increment attempt count
        record.setAttemptCount(record.getAttemptCount() + 1);

        // Simulate verification (in production, call external service)
        boolean verified = simulateVerification(verificationType, fieldValue);

        if (verified) {
            record.setStatus("SUCCESS");
            record.setVerifiedAt(LocalDateTime.now());
        } else {
            record.setStatus("FAILED");
        }

        verificationRecordRepository.save(record);

        return VerificationResult.builder()
                .verified(verified)
                .message(verified ? "Verification successful" : "Verification failed")
                .build();
    }

    /**
     * Simulate verification for MVP.
     * In production, this would call actual verification services.
     */
    private boolean simulateVerification(String verificationType, Object fieldValue) {
        log.debug("Simulating verification for type: {}", verificationType);
        
        // For MVP, always return true
        // In production, implement actual verification logic
        return true;
    }
}

