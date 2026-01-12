package com.los.config.entity;

import com.los.config.converter.JsonConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable snapshot of flow and screen configurations for an application.
 * Created on first screen submission and never modified.
 */
@Entity
@Table(name = "flow_snapshots")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "flow_config_id", nullable = false)
    private Long flowConfigId;

    @Column(name = "snapshot_data", columnDefinition = "TEXT", nullable = false)
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> snapshotData;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

