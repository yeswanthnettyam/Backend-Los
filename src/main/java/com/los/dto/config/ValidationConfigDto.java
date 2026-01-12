package com.los.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Validation Configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationConfigDto {

    private Long configId;

    @NotBlank(message = "Screen ID is required")
    private String screenId;

    private String productCode;
    private String partnerCode;
    private String branchCode;

    private Integer version;

    @Builder.Default
    private String status = "DRAFT"; // Defaults to DRAFT if not provided

    @NotNull(message = "Validation rules are required")
    private Map<String, Object> validationRules;

    private String createdBy;
    private String updatedBy;
}

