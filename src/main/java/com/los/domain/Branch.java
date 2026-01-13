package com.los.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Master data entity representing a branch.
 */
@Entity
@Table(name = "BRANCHES")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @Column(name = "BRANCH_CODE", length = 50)
    private String branchCode;

    @Column(name = "BRANCH_NAME", nullable = false, length = 255)
    private String branchName;

    @Column(name = "PARTNER_CODE", nullable = false, length = 50)
    private String partnerCode;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}
