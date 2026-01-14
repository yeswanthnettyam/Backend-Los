package com.los.controller;

import com.los.dto.runtime.NextScreenRequest;
import com.los.dto.runtime.NextScreenResponse;
import com.los.service.RuntimeOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Runtime API controller for screen navigation and orchestration.
 */
@RestController
@RequestMapping("/api/v1/runtime")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Runtime API", description = "Runtime orchestration for screen navigation")
public class RuntimeController {

    private final RuntimeOrchestrationService orchestrationService;

    @Operation(summary = "Process screen submission and get next screen (supports flow start)")
    @PostMapping("/next-screen")
    public ResponseEntity<NextScreenResponse> nextScreen(@Valid @RequestBody NextScreenRequest request) {
        // Determine if this is flow start or screen progression
        boolean isFlowStart = (request.getCurrentScreenId() == null || request.getCurrentScreenId().isBlank());
        
        if (isFlowStart) {
            // Flow start requires flowId, productCode, and partnerCode
            java.util.List<com.los.dto.runtime.ValidationErrorResponse.FieldError> errors = new java.util.ArrayList<>();
            
            if (request.getFlowId() == null || request.getFlowId().isBlank()) {
                errors.add(com.los.dto.runtime.ValidationErrorResponse.FieldError.builder()
                        .fieldId("flowId")
                        .code("REQUIRED")
                        .message("Flow ID is required for flow start")
                        .build());
            }
            if (request.getProductCode() == null || request.getProductCode().isBlank()) {
                errors.add(com.los.dto.runtime.ValidationErrorResponse.FieldError.builder()
                        .fieldId("productCode")
                        .code("REQUIRED")
                        .message("Product code is required for flow start")
                        .build());
            }
            if (request.getPartnerCode() == null || request.getPartnerCode().isBlank()) {
                errors.add(com.los.dto.runtime.ValidationErrorResponse.FieldError.builder()
                        .fieldId("partnerCode")
                        .code("REQUIRED")
                        .message("Partner code is required for flow start")
                        .build());
            }
            
            if (!errors.isEmpty()) {
                throw new com.los.exception.ValidationException(errors);
            }
            
            log.info("Flow start request received for flowId={}, productCode={}, partnerCode={}, branchCode={}", 
                    request.getFlowId(), request.getProductCode(), request.getPartnerCode(), request.getBranchCode());
        } else {
            // Screen progression requires applicationId and currentScreenId
            java.util.List<com.los.dto.runtime.ValidationErrorResponse.FieldError> errors = new java.util.ArrayList<>();
            
            if (request.getApplicationId() == null) {
                errors.add(com.los.dto.runtime.ValidationErrorResponse.FieldError.builder()
                        .fieldId("applicationId")
                        .code("REQUIRED")
                        .message("Application ID is required for screen progression")
                        .build());
            }
            if (request.getCurrentScreenId() == null || request.getCurrentScreenId().isBlank()) {
                errors.add(com.los.dto.runtime.ValidationErrorResponse.FieldError.builder()
                        .fieldId("currentScreenId")
                        .code("REQUIRED")
                        .message("Current screen ID is required for screen progression")
                        .build());
            }
            
            if (!errors.isEmpty()) {
                throw new com.los.exception.ValidationException(errors);
            }
            
            log.info("Screen progression request for application={}, currentScreen={}", 
                    request.getApplicationId(), request.getCurrentScreenId());
        }
        
        NextScreenResponse response = orchestrationService.processNextScreen(request);
        
        return ResponseEntity.ok(response);
    }
}

