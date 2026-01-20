package com.los.service;

import com.los.domain.UploadedFile;
import com.los.dto.runtime.FileUploadResponse;
import com.los.repository.LoanApplicationRepository;
import com.los.repository.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for handling file uploads (camera uploads, documents, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final UploadedFileRepository uploadedFileRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Value("${file.upload.directory:${user.dir}/data/uploads}")
    private String uploadDirectory;

    @Value("${file.upload.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${file.upload.base-url:http://localhost:8080/api/v1/files}")
    private String baseUrl;

    private static final String[] ALLOWED_MIME_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png"
    };

    /**
     * Upload a file and persist metadata.
     * 
     * @param file The multipart file
     * @param applicationId The application ID
     * @param screenId The screen ID
     * @param fieldId The field ID
     * @param fileType The file type (CAMERA, DOCUMENT, etc.)
     * @return FileUploadResponse with fileId and fileUrl
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, Long applicationId, String screenId, 
                                        String fieldId, String fileType) {
        log.info("Uploading file for applicationId={}, screenId={}, fieldId={}, fileType={}", 
                applicationId, screenId, fieldId, fileType);

        // Validate application exists
        loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Validate file
        validateFile(file);

        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = generateUniqueFileName(applicationId, screenId, fieldId, fileExtension);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDirectory, e);
            throw new RuntimeException("Failed to create upload directory", e);
        }

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFileName);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new RuntimeException("Failed to save file", e);
        }

        // Generate file URL
        String fileUrl = baseUrl + "/" + uniqueFileName;

        // Persist metadata
        UploadedFile uploadedFile = UploadedFile.builder()
                .applicationId(applicationId)
                .screenId(screenId)
                .fieldId(fieldId)
                .fileType(fileType != null ? fileType : "CAMERA")
                .fileName(originalFileName)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .filePath(filePath.toString())
                .fileUrl(fileUrl)
                .build();

        uploadedFile = uploadedFileRepository.save(uploadedFile);
        log.info("File metadata persisted with fileId={}", uploadedFile.getFileId());

        // Mask sensitive data in logs
        log.info("File upload successful: fileId={}, applicationId={}, screenId={}, fieldId={}, " +
                "fileName={}, fileSize={}, mimeType={}", 
                uploadedFile.getFileId(), applicationId, screenId, fieldId,
                originalFileName, file.getSize(), file.getContentType());

        return FileUploadResponse.builder()
                .fileId(uploadedFile.getFileId())
                .fileUrl(fileUrl)
                .fileName(originalFileName)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
    }

    /**
     * Check if required camera fields are uploaded for a screen.
     * 
     * @param applicationId The application ID
     * @param screenId The screen ID
     * @param requiredFields List of field IDs that require camera uploads
     * @return true if all required fields have uploads, false otherwise
     */
    public boolean areRequiredCameraFieldsUploaded(Long applicationId, String screenId, 
                                                   java.util.List<String> requiredFields) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return true; // No required fields
        }

        for (String fieldId : requiredFields) {
            boolean exists = uploadedFileRepository.existsByApplicationIdAndScreenIdAndFieldId(
                    applicationId, screenId, fieldId);
            if (!exists) {
                log.warn("Required camera field not uploaded: applicationId={}, screenId={}, fieldId={}", 
                        applicationId, screenId, fieldId);
                return false;
            }
        }

        return true;
    }

    /**
     * Get uploaded files for an application and screen.
     */
    public java.util.List<UploadedFile> getUploadedFiles(Long applicationId, String screenId) {
        return uploadedFileRepository.findByApplicationIdAndScreenId(applicationId, screenId);
    }

    /**
     * Validate file: MIME type, size, etc.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        boolean isValidMimeType = false;
        for (String allowedType : ALLOWED_MIME_TYPES) {
            if (allowedType.equals(contentType)) {
                isValidMimeType = true;
                break;
            }
        }

        if (!isValidMimeType) {
            log.warn("Invalid MIME type: {}", contentType);
            throw new IllegalArgumentException("Invalid file type. Allowed types: image/jpeg, image/png");
        }

        // Validate file size
        if (file.getSize() > maxFileSize) {
            log.warn("File size exceeds limit: {} bytes (max: {} bytes)", file.getSize(), maxFileSize);
            throw new IllegalArgumentException("File size exceeds maximum allowed size: " + maxFileSize + " bytes");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String generateUniqueFileName(Long applicationId, String screenId, String fieldId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%d_%s_%s_%s_%s%s", 
                applicationId, screenId, fieldId, timestamp, uuid, extension);
    }
}
