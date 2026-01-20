package com.los.dto.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for file upload API.
 * Note: The actual file is sent as multipart/form-data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadRequest {
    
    private Long applicationId;
    private String screenId;
    private String fieldId;
    private String fileType; // CAMERA, DOCUMENT, etc.
}
