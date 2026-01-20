package com.los.controller;

import com.los.dto.runtime.WebViewInitRequest;
import com.los.dto.runtime.WebViewInitResponse;
import com.los.service.ConfigResolutionService;
import com.los.service.WebViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for WebView initialization.
 * Provides simple URL launcher - no callbacks, no tracking, no blocking.
 */
@RestController
@RequestMapping("/api/v1/webview")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebView API", description = "WebView initialization endpoints for external links")
public class WebViewController {

    private final WebViewService webViewService;
    private final ConfigResolutionService configResolutionService;

    @Operation(summary = "Initialize WebView and get launch URL")
    @PostMapping("/init")
    public ResponseEntity<WebViewInitResponse> initWebView(@RequestBody WebViewInitRequest request) {
        log.info("WebView init request: applicationId={}, screenId={}, fieldId={}", 
                request.getApplicationId(), request.getScreenId(), request.getFieldId());

        // Get screen config to extract WebView configuration
        Map<String, Object> screenConfig = configResolutionService.getScreenConfig(
                request.getScreenId(), null, null, null);

        if (screenConfig == null) {
            throw new IllegalArgumentException("Screen config not found: " + request.getScreenId());
        }

        // Extract WebView config from screen config
        @SuppressWarnings("unchecked")
        Map<String, Object> uiConfig = (Map<String, Object>) screenConfig.get("uiConfig");
        if (uiConfig == null) {
            throw new IllegalArgumentException("uiConfig not found in screen config");
        }

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> fields = (java.util.List<Map<String, Object>>) uiConfig.get("fields");
        if (fields == null) {
            throw new IllegalArgumentException("fields not found in uiConfig");
        }

        // Find the field with WebView configuration
        Map<String, Object> webViewConfig = null;
        for (Map<String, Object> field : fields) {
            String fieldId = (String) field.get("id");
            if (request.getFieldId().equals(fieldId)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> webView = (Map<String, Object>) field.get("webView");
                if (webView != null) {
                    webViewConfig = webView;
                    break;
                }
            }
        }

        if (webViewConfig == null) {
            throw new IllegalArgumentException("WebView configuration not found for field: " + request.getFieldId());
        }

        WebViewInitResponse response = webViewService.initWebView(
                request.getApplicationId(),
                request.getScreenId(),
                request.getFieldId(),
                webViewConfig);

        log.info("WebView init successful: applicationId={}, screenId={}, fieldId={}", 
                request.getApplicationId(), request.getScreenId(), request.getFieldId());

        return ResponseEntity.ok(response);
    }
}
