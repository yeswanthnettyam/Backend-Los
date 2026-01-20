package com.los.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing uploaded files (camera uploads, documents, etc.)
 */
@Entity
@Table(name = "uploaded_files")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "screen_id", nullable = false, length = 100)
    private String screenId;

    @Column(name = "field_id", nullable = false, length = 100)
    private String fieldId;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType; // CAMERA, DOCUMENT, etc.

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
