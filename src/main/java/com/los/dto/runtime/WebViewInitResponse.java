package com.los.dto.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for WebView init API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebViewInitResponse {
    
    private String url;
}
