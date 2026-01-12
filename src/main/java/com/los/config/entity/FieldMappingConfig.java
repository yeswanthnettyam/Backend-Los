package com.los.config.entity;

import com.los.config.converter.JsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Configuration entity for field mappings.
 * Defines how UI fields map to domain entities.
 */
@Entity
@Table(name = "field_mapping_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"screen_id", "product_code", "partner_code", "branch_code", "version"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMappingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "screen_id", nullable = false, length = 100)
    private String screenId;

    @Column(name = "product_code", length = 50)
    private String productCode;

    @Column(name = "partner_code", length = 50)
    private String partnerCode;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "mappings", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> mappings;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "lock_version")
    private Integer lockVersion;
}

