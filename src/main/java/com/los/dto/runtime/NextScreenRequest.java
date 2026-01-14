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

    /**
     * Form data submitted from the current screen.
     * Can be null or empty for flow start (first screen has no data yet).
     * Required for screen progression (validation and field mapping).
     */
    private Map<String, Object> formData;

    /**
     * Flow ID is required for flow start (when currentScreenId is null).
     * For screen progression, can be retrieved from application's flow snapshot.
     */
    private String flowId;

    /**
     * Product code is required for flow start (when currentScreenId is null).
     * For screen progression, can be retrieved from application entity.
     */
    private String productCode;

    /**
     * Partner code is required for flow start (when currentScreenId is null).
     * For screen progression, can be retrieved from application entity.
     */
    private String partnerCode;

    private String branchCode;
}

