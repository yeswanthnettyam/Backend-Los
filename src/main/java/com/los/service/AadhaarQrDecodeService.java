package com.los.service;

import com.los.dto.runtime.AadhaarQrDecodeResponse;
import com.los.service.qr.UidaiSecureQrDecoder;
import com.los.util.CorrelationIdHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for decoding Aadhaar Secure QR codes.
 * Aadhaar Secure QR is a numeric string (digits only), not Base64.
 * Converts numeric string to bytes and delegates to UIDAI decoder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AadhaarQrDecodeService {

    private final UidaiSecureQrDecoder qrDecoder;

    private static final int MIN_QR_PAYLOAD_LENGTH = 1001; // Minimum length for valid Aadhaar QR numeric string

    /**
     * Decode Aadhaar Secure QR from numeric string payload.
     * 
     * @param qrPayload Numeric string QR payload (digits only)
     * @return Decoded Aadhaar data (last 4 digits only)
     * @throws IllegalArgumentException if payload is invalid (not numeric or too short)
     * @throws UidaiSecureQrDecoder.QrDecodeException if QR decoding fails
     */
    public AadhaarQrDecodeResponse decodeAadhaarQr(String qrPayload) 
            throws UidaiSecureQrDecoder.QrDecodeException {
        String correlationId = CorrelationIdHolder.get();
        log.info("[{}] Decoding Aadhaar QR payload (numeric string length: {})", 
                correlationId, qrPayload != null ? qrPayload.length() : 0);

        // Validate input
        if (qrPayload == null || qrPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("QR payload is required");
        }

        String trimmedPayload = qrPayload.trim();

        // Validate numeric string format
        if (!trimmedPayload.matches("^[0-9]+$")) {
            log.error("[{}] QR payload contains non-numeric characters", correlationId);
            throw new IllegalArgumentException("QR payload must contain only digits (0-9)");
        }

        // Validate minimum length
        if (trimmedPayload.length() < MIN_QR_PAYLOAD_LENGTH) {
            log.error("[{}] QR payload too short: {} characters (minimum: {} characters)", 
                    correlationId, trimmedPayload.length(), MIN_QR_PAYLOAD_LENGTH);
            throw new IllegalArgumentException(
                    String.format("QR payload must be at least %d characters", MIN_QR_PAYLOAD_LENGTH));
        }

        // Convert numeric string to bytes (UTF-8 encoding)
        byte[] qrBytes = trimmedPayload.getBytes(StandardCharsets.UTF_8);
        log.info("[{}] Converted numeric string to {} bytes", correlationId, qrBytes.length);

        // Decode UIDAI Secure QR
        UidaiSecureQrDecoder.DecodedAadhaarData decodedData;
        try {
            decodedData = qrDecoder.decode(qrBytes);
        } catch (UidaiSecureQrDecoder.QrDecodeException e) {
            log.error("[{}] Failed to decode Aadhaar QR: {}", correlationId, e.getMessage());
            throw e;
        }

        // Build response with all decoded fields
        String aadhaarNumber = decodedData.getAadhaarNumber();
        String aadhaarLast4 = extractLast4Digits(aadhaarNumber);
        String formattedAddress = formatAddress(decodedData);

        log.info(
                "Successfully decoded Aadhaar QR: name={}, gender={}, dob={}, aadhaarLast4={}, address={}",
                maskName(decodedData.getName()),
                decodedData.getGender(),
                decodedData.getDob(),
                aadhaarLast4,
                formattedAddress
        );

        return AadhaarQrDecodeResponse.builder()
                // Demographic
                .name(decodedData.getName())
                .gender(decodedData.getGender())
                .dob(decodedData.getDob())
                .yob(decodedData.getYob())
                .aadhaarLast4(aadhaarLast4)
                // Address components
                .careOf(decodedData.getCareOf())
                .house(decodedData.getHouse())
                .landmark(decodedData.getLandmark())
                .location(decodedData.getLocation())
                .street(decodedData.getStreet())
                .subDistrict(decodedData.getSubDistrict())
                .district(decodedData.getDistrict())
                .state(decodedData.getState())
                .pinCode(decodedData.getPinCode())
                .postOffice(decodedData.getPostOffice())
                .vtc(decodedData.getVtc())
                // Formatted address
                .address(formattedAddress)
                // Contact hashes
                .emailHash(decodedData.getEmailHash())
                .mobileHash(decodedData.getMobileHash())
                // Security (signature present but not verified)
                .signature(decodedData.getSignature())
                .build();
    }
    /**
     * Format address from decoded data.
     * Format: "District, State, PIN-XXXXXX"
     */
    private String formatAddress(UidaiSecureQrDecoder.DecodedAadhaarData data) {
        if (data == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();

        if (hasText(data.getDistrict())) {
            parts.add(data.getDistrict());
        }
        if (hasText(data.getState())) {
            parts.add(data.getState());
        }
        if (hasText(data.getPinCode())) {
            parts.add("PIN-" + data.getPinCode());
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }


    /**
     * Extract last 4 digits from Aadhaar number.
     */
    private String extractLast4Digits(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.trim().isEmpty()) {
            return null;
        }

        String digitsOnly = aadhaarNumber.replaceAll("\\s", "");
        if (digitsOnly.length() >= 4) {
            return digitsOnly.substring(digitsOnly.length() - 4);
        }

        return digitsOnly;
    }

    /**
     * Mask name for logging (show first letter only).
     */
    private String maskName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 1) {
            return trimmed;
        }
        return trimmed.charAt(0) + "***";
    }
}
