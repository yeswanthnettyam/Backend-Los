package com.los.dto.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for successful next-screen request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextScreenResponse {

    private Long applicationId;
    private String nextScreenId;
    private Map<String, Object> screenConfig;
    private String status;
}

