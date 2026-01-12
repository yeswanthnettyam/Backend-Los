package com.los.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Result of a verification check.
 */
@Data
@AllArgsConstructor
@Builder
public class VerificationResult {

    private boolean verified;
    private String message;
}

