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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextScreenRequest {

    private Long applicationId;

    @NotBlank(message = "Current screen ID is required")
    private String currentScreenId;

    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Partner code is required")
    private String partnerCode;

    private String branchCode;
}

