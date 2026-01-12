package com.los.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Flow Configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowConfigDto {

    private Long configId;

    @NotBlank(message = "Flow ID is required")
    private String flowId;

    private String productCode;
    private String partnerCode;
    private String branchCode;

    private Integer version;

    @Builder.Default
    private String status = "DRAFT"; // Defaults to DRAFT if not provided

    @NotNull(message = "Flow definition is required")
    private Map<String, Object> flowDefinition;

    private String createdBy;
    private String updatedBy;
}

