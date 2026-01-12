package com.los.dto.runtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for the runtime next-screen API.
 * Supports both flow start (currentScreenId = null) and screen progression (currentScreenId != null).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextScreenRequest {

    private Long applicationId;

    /**
     * Current screen ID. NULL indicates flow start.
     * When NULL, the API will:
     * - Resolve the active flow config
     * - Get the start screen from flow definition
     * - Create a flow snapshot
     * - Return the first screen config
     */
    private String currentScreenId;

    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;

    /**
     * Flow ID is required for flow start.
     * Used to resolve the correct flow configuration.
     */
    @NotBlank(message = "Flow ID is required")
    private String flowId;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Partner code is required")
    private String partnerCode;

    private String branchCode;
}

