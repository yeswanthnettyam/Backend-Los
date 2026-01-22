package com.los.dto.runtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Aadhaar Secure QR decoding.
 * Aadhaar Secure QR is a numeric string (digits only), not Base64.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AadhaarQrDecodeRequest {

    /**
     * QR payload as numeric string (digits only).
     * Must contain only digits (0-9) and have length > 1000.
     */
    @NotBlank(message = "QR payload is required")
    @Pattern(regexp = "^[0-9]+$", message = "QR payload must contain only digits")
    @Size(min = 1001, message = "QR payload must be at least 1001 characters")
    private String qrPayload;
}
