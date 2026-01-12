package com.los.service;

import com.los.domain.LoanApplication;
import com.los.dto.runtime.NextScreenRequest;
import com.los.dto.runtime.NextScreenResponse;
import com.los.flow.FlowEngine;
import com.los.mapping.FieldMappingEngine;
import com.los.repository.*;
import com.los.validation.ValidationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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

    /**
     * Process screen submission and determine next screen.
     * This is the main entry point for the runtime API.
     */
    @Transactional
    public NextScreenResponse processNextScreen(NextScreenRequest request) {
        log.info("Processing next screen for application: {}", request.getApplicationId());

        // Get or create application
        LoanApplication application = getOrCreateApplication(request);

        // Get validation config
        Map<String, Object> validationConfig = configResolutionService.getValidationConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );

        // Step 1: Validate form data
        log.debug("Validating form data for screen: {}", request.getCurrentScreenId());
        validationEngine.validate(request.getFormData(), validationConfig);

        // Step 2: Apply field mappings and persist
        log.debug("Applying field mappings");
        Map<String, Object> mappingConfig = configResolutionService.getFieldMappingConfig(
                request.getCurrentScreenId(),
                application.getProductCode(),
                application.getPartnerCode(),
                application.getBranchCode()
        );
        fieldMappingEngine.applyMappings(application.getApplicationId(), request.getFormData(), mappingConfig);

        // Step 3: Determine next screen
        log.debug("Determining next screen");
        String nextScreenId = flowEngine.getNextScreen(application, request.getCurrentScreenId(), request.getFormData());

        // Update application status
        if (nextScreenId != null) {
            application.setCurrentScreenId(nextScreenId);
            application.setStatus("IN_PROGRESS");
        } else {
            // End of flow
            application.setStatus("COMPLETED");
        }
        loanApplicationRepository.save(application);

        // Step 4: Get next screen config
        Map<String, Object> screenConfig = null;
        if (nextScreenId != null) {
            screenConfig = flowEngine.getScreenConfig(application, nextScreenId);
        }

        // Build response
        return NextScreenResponse.builder()
                .applicationId(application.getApplicationId())
                .nextScreenId(nextScreenId)
                .screenConfig(screenConfig)
                .status(application.getStatus())
                .build();
    }

    /**
     * Get existing application or create new one.
     */
    private LoanApplication getOrCreateApplication(NextScreenRequest request) {
        if (request.getApplicationId() != null) {
            return loanApplicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found"));
        }

        // Create new application
        LoanApplication application = LoanApplication.builder()
                .productCode(request.getProductCode())
                .partnerCode(request.getPartnerCode())
                .branchCode(request.getBranchCode())
                .status("INITIATED")
                .currentScreenId(request.getCurrentScreenId())
                .build();

        return loanApplicationRepository.save(application);
    }
}

