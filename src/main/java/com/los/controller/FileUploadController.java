package com.los.controller;

import com.los.dto.runtime.FileUploadRequest;
import com.los.dto.runtime.FileUploadResponse;
import com.los.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for file upload operations (camera uploads, documents, etc.)
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload API", description = "File upload endpoints for camera uploads and documents")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "Upload a file (camera upload, document, etc.)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("screenId") String screenId,
            @RequestParam("fieldId") String fieldId,
            @RequestParam(value = "fileType", defaultValue = "CAMERA") String fileType) {
        
        log.info("File upload request: applicationId={}, screenId={}, fieldId={}, fileType={}, " +
                "fileName={}, fileSize={}, contentType={}", 
                applicationId, screenId, fieldId, fileType,
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        FileUploadResponse response = fileUploadService.uploadFile(
                file, applicationId, screenId, fieldId, fileType);

        log.info("File upload successful: fileId={}, fileUrl={}", response.getFileId(), response.getFileUrl());
        
        return ResponseEntity.ok(response);
    }
}
