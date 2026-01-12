package com.los.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Stores verification results (OTP, PAN, Aadhaar, etc.) per application.
 */
@Entity
@Table(name = "verification_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "field_id", nullable = false, length = 100)
    private String fieldId;

    @Column(name = "verification_type", nullable = false, length = 50)
    private String verificationType; // OTP, PAN, AADHAAR, BUREAU

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}

