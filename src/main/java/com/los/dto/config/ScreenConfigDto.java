package com.los.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Screen Configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenConfigDto {

    private Long configId;

    @NotBlank(message = "Screen ID is required")
    private String screenId;

    private String productCode;
    private String partnerCode;
    private String branchCode;

    private Integer version;

    @Builder.Default
    private String status = "DRAFT"; // Defaults to DRAFT if not provided

    @NotNull(message = "UI config is required")
    private Map<String, Object> uiConfig;

    private String createdBy;
    private String updatedBy;
}

