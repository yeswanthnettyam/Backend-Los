package com.los.service;

import com.los.dto.runtime.WebViewInitResponse;
import com.los.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for WebView URL generation.
 * Supports STATIC and API-based URL modes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebViewService {

    private final LoanApplicationRepository loanApplicationRepository;

    /**
     * Initialize WebView and return launch URL.
     * 
     * This is a SIMPLE URL launcher - no callbacks, no tracking, no blocking.
     * 
     * @param applicationId The application ID
     * @param screenId The screen ID
     * @param fieldId The field ID
     * @param webViewConfig The WebView configuration from screen config
     * @return WebViewInitResponse with launch URL
     */
    public WebViewInitResponse initWebView(Long applicationId, String screenId, String fieldId,
                                           Map<String, Object> webViewConfig) {
        log.info("Initializing WebView: applicationId={}, screenId={}, fieldId={}", 
                applicationId, screenId, fieldId);

        // Validate application exists
        loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (webViewConfig == null || webViewConfig.isEmpty()) {
            throw new IllegalArgumentException("WebView configuration is missing for field: " + fieldId);
        }

        String urlSource = (String) webViewConfig.get("urlSource");
        if (urlSource == null) {
            throw new IllegalArgumentException("urlSource is required in WebView configuration");
        }

        String url;
        if ("STATIC".equalsIgnoreCase(urlSource)) {
            // STATIC URL mode - return URL directly from config
            url = (String) webViewConfig.get("url");
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("url is required for STATIC urlSource");
            }
            log.info("WebView STATIC URL: {}", maskUrl(url));
        } else if ("API".equalsIgnoreCase(urlSource)) {
            // API-BASED URL mode - call vendor API or generate URL
            url = generateApiBasedUrl(applicationId, screenId, fieldId, webViewConfig);
            log.info("WebView API-based URL generated: {}", maskUrl(url));
        } else {
            throw new IllegalArgumentException("Unsupported urlSource: " + urlSource + ". Supported: STATIC, API");
        }

        // Log event (mask sensitive data)
        log.info("WebView init successful: applicationId={}, screenId={}, fieldId={}, urlSource={}", 
                applicationId, screenId, fieldId, urlSource);

        return WebViewInitResponse.builder()
                .url(url)
                .build();
    }

    /**
     * Generate URL for API-based WebView mode.
     * This can call vendor APIs, generate signed URLs, etc.
     */
    private String generateApiBasedUrl(Long applicationId, String screenId, String fieldId,
                                       Map<String, Object> webViewConfig) {
        // For MVP, return a placeholder URL
        // In production, this would:
        // 1. Call vendor API with application context
        // 2. Generate signed URLs
        // 3. Handle authentication tokens
        // 4. Never store vendor secrets
        
        String launchApi = (String) webViewConfig.get("launchApi");
        if (launchApi == null || launchApi.isEmpty()) {
            throw new IllegalArgumentException("launchApi is required for API urlSource");
        }

        // Example: Generate URL based on application context
        // In real implementation, this would call vendor API
        String baseUrl = "https://vendor.example.com/launch";
        String url = String.format("%s?applicationId=%d&screenId=%s&fieldId=%s", 
                baseUrl, applicationId, screenId, fieldId);

        log.debug("Generated API-based URL for applicationId={}, screenId={}, fieldId={}", 
                applicationId, screenId, fieldId);

        return url;
    }

    /**
     * Mask URL in logs for security.
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 20) {
            return url;
        }
        return url.substring(0, 20) + "***";
    }
}
