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
        if (request.getCurrentScreenId() == null) {
            log.info("Flow start request received for flowId={}, productCode={}, partnerCode={}, branchCode={}", 
                    request.getFlowId(), request.getProductCode(), request.getPartnerCode(), request.getBranchCode());
        } else {
            log.info("Screen progression request for application={}, currentScreen={}", 
                    request.getApplicationId(), request.getCurrentScreenId());
        }
        
        NextScreenResponse response = orchestrationService.processNextScreen(request);
        
        return ResponseEntity.ok(response);
    }
}

