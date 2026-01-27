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
        if (request.getCurrentScreenId() == null) {
            return handleFlowStart(request);
        } else {
            return handleScreenProgression(request);
        }
    }

    private NextScreenResponse handleFlowStart(NextScreenRequest request) {

        LoanApplication application = LoanApplication.builder()
                .productCode(request.getProductCode())
                .partnerCode(request.getPartnerCode())
                .branchCode(request.getBranchCode())
                .status("INITIATED")
                .build();
        application = loanApplicationRepository.save(application);

        String startScreenId = flowEngine.getStartScreen(application, request.getFlowId());
        application.setCurrentScreenId(startScreenId);
        loanApplicationRepository.save(application);

        Map<String, Object> screenConfig = flowEngine.getScreenConfig(application, startScreenId);
        return NextScreenResponse.builder()
                .applicationId(application.getApplicationId())
                .nextScreenId(startScreenId)
                .screenConfig(screenConfig)
                .status(application.getStatus())
                .build();
    }

    private NextScreenResponse handleScreenProgression(NextScreenRequest request) {
        LoanApplication application = getOrCreateApplication(request);

        Map<String, Object> formData = request.getFormData();
        if (formData == null) {
            formData = new HashMap<>();
        }

        Map<String, Object> validationConfig = configResolutionService.getValidationConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );

        if (validationConfig != null) {
            validationEngine.validate(formData, validationConfig, request.getCurrentScreenId(), 
                    application.getProductCode(), application.getPartnerCode(), application.getBranchCode());
        }

        validateRequiredCameraUploads(application.getApplicationId(), request.getCurrentScreenId());

        Map<String, Object> mappingConfig = configResolutionService.getFieldMappingConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        fieldMappingEngine.applyMappings(application.getApplicationId(), formData, mappingConfig);

        String nextScreenId = flowEngine.getNextScreen(application, request.getCurrentScreenId(), formData, request.getFlowId());

        if (nextScreenId != null) {
            application.setCurrentScreenId(nextScreenId);
            application.setStatus("IN_PROGRESS");
        } else {
            application.setStatus("COMPLETED");
        }
        loanApplicationRepository.save(application);

        Map<String, Object> screenConfig = null;
        if (nextScreenId != null) {
            screenConfig = flowEngine.getScreenConfig(application, nextScreenId);
        }

        return NextScreenResponse.builder()
                .applicationId(application.getApplicationId())
                .nextScreenId(nextScreenId)
                .screenConfig(screenConfig)
                .status(application.getStatus())
                .build();
    }

    private LoanApplication getOrCreateApplication(NextScreenRequest request) {
        if (request.getApplicationId() != null) {
            return loanApplicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found: " + request.getApplicationId()));
        }

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
            return existingApplication.get();
        }
        
        LoanApplication application = LoanApplication.builder()
                .productCode(request.getProductCode())
                .partnerCode(request.getPartnerCode())
                .branchCode(request.getBranchCode())
                .status("INITIATED")
                .currentScreenId(request.getCurrentScreenId())
                .build();

        return loanApplicationRepository.save(application);
    }

    @SuppressWarnings("unchecked")
    private void validateRequiredCameraUploads(Long applicationId, String screenId) {
        Map<String, Object> screenConfig = configResolutionService.getScreenConfig(
                screenId, null, null, null);

        if (screenConfig == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> uiConfig = (Map<String, Object>) screenConfig.get("uiConfig");
        if (uiConfig == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) uiConfig.get("fields");
        if (fields == null || fields.isEmpty()) {
            return;
        }

        List<String> requiredCameraFields = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String fieldType = (String) field.get("type");
            Boolean required = (Boolean) field.get("required");
            
            if ("CAMERA".equalsIgnoreCase(fieldType) && Boolean.TRUE.equals(required)) {
                String fieldId = (String) field.get("id");
                if (fieldId != null) {
                    requiredCameraFields.add(fieldId);
                }
            }
        }

        if (!requiredCameraFields.isEmpty()) {
            boolean allUploaded = fileUploadService.areRequiredCameraFieldsUploaded(
                    applicationId, screenId, requiredCameraFields);
            
            if (!allUploaded) {
                throw new RuntimeException("Required camera uploads are missing. Cannot proceed.");
            }
        }
    }
}

