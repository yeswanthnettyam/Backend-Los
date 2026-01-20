package com.los.service;

import com.los.domain.LoanApplication;
import com.los.dto.runtime.NextScreenRequest;
import com.los.dto.runtime.NextScreenResponse;
import com.los.flow.FlowEngine;
import com.los.mapping.FieldMappingEngine;
import com.los.repository.*;
import com.los.service.FileUploadService;
import com.los.validation.ValidationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main orchestration service for runtime API.
 * Coordinates validation, mapping, flow, and persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuntimeOrchestrationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ValidationEngine validationEngine;
    private final FieldMappingEngine fieldMappingEngine;
    private final FlowEngine flowEngine;
    private final ConfigResolutionService configResolutionService;
    private final FileUploadService fileUploadService;

    /**
     * Process screen submission and determine next screen.
     * This is the main entry point for the runtime API.
     * 
     * Supports two modes:
     * 1. Flow Start (currentScreenId == null): Initializes flow and returns first screen
     * 2. Screen Progression (currentScreenId != null): Validates, maps, and navigates to next screen
     */
    @Transactional
    public NextScreenResponse processNextScreen(NextScreenRequest request) {
        log.info("Processing next screen for application: {}", request.getApplicationId());

        // Branch based on flow start vs. screen progression
        if (request.getCurrentScreenId() == null) {
            return handleFlowStart(request);
        } else {
            return handleScreenProgression(request);
        }
    }

    /**
     * Handle flow start: Create application, snapshot flow, return first screen.
     */
    private NextScreenResponse handleFlowStart(NextScreenRequest request) {
        log.info("Handling flow start for flowId={}", request.getFlowId());

        // Create new application
        LoanApplication application = LoanApplication.builder()
                .productCode(request.getProductCode())
                .partnerCode(request.getPartnerCode())
                .branchCode(request.getBranchCode())
                .status("INITIATED")
                .build();
        application = loanApplicationRepository.save(application);

        // Get start screen from flow and create snapshot
        String startScreenId = flowEngine.getStartScreen(application, request.getFlowId());
        
        // Update application with start screen
        application.setCurrentScreenId(startScreenId);
        loanApplicationRepository.save(application);

        // Get screen config for first screen
        Map<String, Object> screenConfig = flowEngine.getScreenConfig(application, startScreenId);

        // Build response
        return NextScreenResponse.builder()
                .applicationId(application.getApplicationId())
                .nextScreenId(startScreenId)
                .screenConfig(screenConfig)
                .status(application.getStatus())
                .build();
    }

    /**
     * Handle screen progression: Validate, map, persist, navigate to next screen.
     */
    private NextScreenResponse handleScreenProgression(NextScreenRequest request) {
        log.info("Handling screen progression from screenId={}", request.getCurrentScreenId());

        // Get or create application
        LoanApplication application = getOrCreateApplication(request);

        // Handle null/empty formData (use empty map as default)
        Map<String, Object> formData = request.getFormData();
        if (formData == null) {
            formData = new HashMap<>();
            log.debug("Form data is null, using empty map");
        }

        // Get validation config (may be null if not configured)
        Map<String, Object> validationConfig = configResolutionService.getValidationConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );

        // Step 1: Validate form data (skipped if validationConfig is null)
        // WebView fields are automatically ignored by ValidationEngine
        if (validationConfig != null) {
            log.debug("Validating form data for screen: {}", request.getCurrentScreenId());
            validationEngine.validate(formData, validationConfig, request.getCurrentScreenId(), 
                    application.getProductCode(), application.getPartnerCode(), application.getBranchCode());
        } else {
            log.debug("No validation config found for screen: {}. Skipping validation.", request.getCurrentScreenId());
        }

        // Step 1.5: Validate required camera uploads
        validateRequiredCameraUploads(application.getApplicationId(), request.getCurrentScreenId());

        // Step 2: Apply field mappings and persist
        log.debug("Applying field mappings");
        Map<String, Object> mappingConfig = configResolutionService.getFieldMappingConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        fieldMappingEngine.applyMappings(application.getApplicationId(), formData, mappingConfig);

        // Step 3: Determine next screen
        log.info("Determining next screen for application={}, currentScreenId={}, formData keys={}", 
                application.getApplicationId(), request.getCurrentScreenId(), 
                formData != null ? formData.keySet() : "null");
        // Pass flowId to getNextScreen so it can create snapshot if needed
        String nextScreenId = flowEngine.getNextScreen(application, request.getCurrentScreenId(), formData, request.getFlowId());
        log.info("Next screen determined: {} (from currentScreen: {})", nextScreenId, request.getCurrentScreenId());

        // Update application status
        if (nextScreenId != null) {
            application.setCurrentScreenId(nextScreenId);
            application.setStatus("IN_PROGRESS");
            log.info("Updated application {}: currentScreenId={}, status=IN_PROGRESS", 
                    application.getApplicationId(), nextScreenId);
        } else {
            // End of flow
            application.setStatus("COMPLETED");
            log.info("Flow ended for application {}. Status set to COMPLETED", application.getApplicationId());
        }
        loanApplicationRepository.save(application);

        // Step 4: Get next screen config
        Map<String, Object> screenConfig = null;
        if (nextScreenId != null) {
            log.info("Getting screen config for nextScreenId: {}", nextScreenId);
            screenConfig = flowEngine.getScreenConfig(application, nextScreenId);
            log.info("Retrieved screen config for {}: screenId={}", 
                    nextScreenId, screenConfig != null ? screenConfig.get("screenId") : "null");
        }

        // Build response
        NextScreenResponse response = NextScreenResponse.builder()
                .applicationId(application.getApplicationId())
                .nextScreenId(nextScreenId)
                .screenConfig(screenConfig)
                .status(application.getStatus())
                .build();
        
        log.info("Returning response: applicationId={}, nextScreenId={}, status={}", 
                response.getApplicationId(), response.getNextScreenId(), response.getStatus());
        
        return response;
    }

    /**
     * Get existing application or create new one.
     * 
     * When applicationId is null (e.g., first screen submission after flow start),
     * attempts to find the application created during flow start by matching:
     * - productCode, partnerCode, branchCode, and currentScreenId
     * 
     * If not found, creates a new application (fallback for edge cases).
     */
    private LoanApplication getOrCreateApplication(NextScreenRequest request) {
        if (request.getApplicationId() != null) {
            return loanApplicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found: " + request.getApplicationId()));
        }

        // applicationId is null - try to find existing application from flow start
        // This happens when frontend submits first screen but didn't capture applicationId
        log.debug("Application ID is null, attempting to find existing application for first screen submission");
        
        Optional<LoanApplication> existingApplication;
        
        if (request.getBranchCode() != null && !request.getBranchCode().isBlank()) {
            existingApplication = loanApplicationRepository.findFirstByProductCodeAndPartnerCodeAndBranchCodeAndCurrentScreenIdOrderByCreatedAtDesc(
                    request.getProductCode(),
                    request.getPartnerCode(),
                    request.getBranchCode(),
                    request.getCurrentScreenId()
            );
        } else {
            existingApplication = loanApplicationRepository.findFirstByProductCodeAndPartnerCodeAndCurrentScreenIdOrderByCreatedAtDesc(
                    request.getProductCode(),
                    request.getPartnerCode(),
                    request.getCurrentScreenId()
            );
        }
        
        if (existingApplication.isPresent()) {
            LoanApplication app = existingApplication.get();
            log.info("Found existing application ID={} for first screen submission (productCode={}, partnerCode={}, currentScreenId={})",
                    app.getApplicationId(), request.getProductCode(), request.getPartnerCode(), request.getCurrentScreenId());
            return app;
        }
        
        // Not found - this shouldn't happen in normal flow, but create new as fallback
        log.warn("Could not find existing application for first screen submission. Creating new application (productCode={}, partnerCode={}, currentScreenId={}). " +
                "This may indicate the frontend didn't capture applicationId from flow start response.",
                request.getProductCode(), request.getPartnerCode(), request.getCurrentScreenId());
        
        LoanApplication application = LoanApplication.builder()
                .productCode(request.getProductCode())
                .partnerCode(request.getPartnerCode())
                .branchCode(request.getBranchCode())
                .status("INITIATED")
                .currentScreenId(request.getCurrentScreenId())
                .build();

        return loanApplicationRepository.save(application);
    }

    /**
     * Validate that required camera fields are uploaded.
     * Backend MUST re-check - do NOT trust frontend flags.
     */
    @SuppressWarnings("unchecked")
    private void validateRequiredCameraUploads(Long applicationId, String screenId) {
        log.debug("Validating required camera uploads for applicationId={}, screenId={}", 
                applicationId, screenId);

        // Get screen config to find camera fields
        Map<String, Object> screenConfig = configResolutionService.getScreenConfig(
                screenId, null, null, null);

        if (screenConfig == null) {
            log.warn("Screen config not found for screenId={}. Skipping camera validation.", screenId);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uiConfig = (Map<String, Object>) screenConfig.get("uiConfig");
        if (uiConfig == null) {
            log.debug("uiConfig not found. No camera fields to validate.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) uiConfig.get("fields");
        if (fields == null || fields.isEmpty()) {
            log.debug("No fields found. No camera fields to validate.");
            return;
        }

        // Find required camera fields
        List<String> requiredCameraFields = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String fieldType = (String) field.get("type");
            Boolean required = (Boolean) field.get("required");
            
            // Check if field is CAMERA type and required
            if ("CAMERA".equalsIgnoreCase(fieldType) && Boolean.TRUE.equals(required)) {
                String fieldId = (String) field.get("id");
                if (fieldId != null) {
                    requiredCameraFields.add(fieldId);
                }
            }
        }

        // Validate that all required camera fields are uploaded
        if (!requiredCameraFields.isEmpty()) {
            boolean allUploaded = fileUploadService.areRequiredCameraFieldsUploaded(
                    applicationId, screenId, requiredCameraFields);
            
            if (!allUploaded) {
                log.error("Required camera fields not uploaded: applicationId={}, screenId={}, requiredFields={}", 
                        applicationId, screenId, requiredCameraFields);
                throw new RuntimeException("Required camera uploads are missing. Cannot proceed.");
            }
            
            log.info("All required camera fields uploaded: applicationId={}, screenId={}, fields={}", 
                    applicationId, screenId, requiredCameraFields);
        } else {
            log.debug("No required camera fields found for screenId={}", screenId);
        }
    }
}

