package com.los.dto.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Aadhaar Secure QR decoding.
 * Contains normalized Aadhaar data (last 4 digits only for security).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AadhaarQrDecodeResponse {

    // Demographic data
    private String name;
    private String gender;
    private String dob; // Format: DD-MM-YYYY or YYYY-MM-DD (as encoded)
    private String yob; // Year of birth (optional)
    private String aadhaarLast4; // Last 4 digits only (Reference ID)

    // Address components
    private String careOf;
    private String house;
    private String landmark;
    private String location;
    private String street;
    private String subDistrict;
    private String district;
    private String state;
    private String pinCode;
    private String postOffice;
    private String vtc;

    // Formatted address (for convenience)
    private String address; // Formatted as "District, State, PIN-XXXXXX"

    // Contact hashes (UIDAI format - hex strings)
    private String emailHash; // Hex string (if present)
    private String mobileHash; // Hex string (if present)

    // Security
    private String signature; // UIDAI signature (present but not verified)
}
