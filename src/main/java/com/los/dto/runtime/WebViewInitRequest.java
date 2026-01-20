package com.los.dto.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for WebView init API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebViewInitRequest {
    
    private Long applicationId;
    private String screenId;
    private String fieldId;
}
