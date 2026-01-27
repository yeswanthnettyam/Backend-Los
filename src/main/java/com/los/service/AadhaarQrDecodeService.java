package com.los.service;

import com.los.dto.runtime.AadhaarQrDecodeResponse;
import com.los.service.qr.UidaiSecureQrDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service for decoding Aadhaar Secure QR codes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AadhaarQrDecodeService {

    private final UidaiSecureQrDecoder qrDecoder;

    private static final int MIN_QR_PAYLOAD_LENGTH = 1001;
    public AadhaarQrDecodeResponse decodeAadhaarQr(String qrPayload) 
            throws UidaiSecureQrDecoder.QrDecodeException {
        if (qrPayload == null || qrPayload.trim().isEmpty()) {
            throw new IllegalArgumentException("QR payload is required");
        }

        String trimmedPayload = qrPayload.trim();

        if (!trimmedPayload.matches("^[0-9]+$")) {
            throw new IllegalArgumentException("QR payload must contain only digits (0-9)");
        }

        if (trimmedPayload.length() < MIN_QR_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("QR payload must be at least %d characters", MIN_QR_PAYLOAD_LENGTH));
        }

        byte[] qrBytes = trimmedPayload.getBytes(StandardCharsets.UTF_8);

        UidaiSecureQrDecoder.DecodedAadhaarData decodedData;
        try {
            decodedData = qrDecoder.decode(qrBytes);
        } catch (UidaiSecureQrDecoder.QrDecodeException e) {
            log.error("Failed to decode Aadhaar QR: {}", e.getMessage());
            throw e;
        }

        String aadhaarNumber = decodedData.getAadhaarNumber();
        String aadhaarLast4 = extractLast4Digits(aadhaarNumber);
        String formattedAddress = formatAddress(decodedData);

        return AadhaarQrDecodeResponse.builder()
                .name(decodedData.getName())
                .gender(decodedData.getGender())
                .dob(decodedData.getDob())
                .yob(decodedData.getYob())
                .aadhaarLast4(aadhaarLast4)
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
                .address(formattedAddress)
                .emailHash(decodedData.getEmailHash())
                .mobileHash(decodedData.getMobileHash())
                .signature(decodedData.getSignature())
                .build();
    }

    private String formatAddress(UidaiSecureQrDecoder.DecodedAadhaarData data) {
        if (data == null) {
            return null;
        }

        List<String> parts = new java.util.ArrayList<>();
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

    private String extractLast4Digits(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.trim().isEmpty()) {
            return null;
        }
        String digitsOnly = aadhaarNumber.replaceAll("\\s", "");
        return digitsOnly.length() >= 4 ? digitsOnly.substring(digitsOnly.length() - 4) : digitsOnly;
    }
}
