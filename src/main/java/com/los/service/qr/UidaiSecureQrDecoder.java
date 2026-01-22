package com.los.service.qr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstraction for UIDAI Secure QR decoder.
 *
 * NOTE:
 * - This represents parsed data from UIDAI Secure QR
 * - This does NOT imply UIDAI verification or authentication
 * - Aadhaar number contains ONLY last 4 digits (Reference ID)
 */
public interface UidaiSecureQrDecoder {

    DecodedAadhaarData decode(byte[] qrBytes) throws QrDecodeException;

    /* =========================================================
       DECODED DATA MODEL (Android-aligned, backend-safe)
       ========================================================= */

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class DecodedAadhaarData {

        // --- Demographic ---
        private String name;
        private String gender;
        private String dob;        // DD-MM-YYYY or YYYY-MM-DD (as encoded)
        private String yob;        // Optional (some QR versions)
        private String aadhaarNumber; // Last 4 digits ONLY (Reference ID)

        // --- Address ---
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

        // --- Contact (hashed, UIDAI format) ---
        private String emailHash;   // Hex string
        private String mobileHash;  // Hex string

        // --- Security ---
        private String signature;   // Raw 256-byte UIDAI signature (ISO-8859-1)
    }

    /* =========================================================
       EXCEPTION
       ========================================================= */

    class QrDecodeException extends Exception {
        private final QrDecodeErrorType errorType;

        public QrDecodeException(String message) {
            super(message);
            this.errorType = QrDecodeErrorType.UNKNOWN;
        }

        public QrDecodeException(String message, Throwable cause) {
            super(message, cause);
            this.errorType = QrDecodeErrorType.UNKNOWN;
        }

        public QrDecodeException(String message, QrDecodeErrorType errorType) {
            super(message);
            this.errorType = errorType;
        }

        public QrDecodeException(String message, QrDecodeErrorType errorType, Throwable cause) {
            super(message, cause);
            this.errorType = errorType;
        }

        public QrDecodeErrorType getErrorType() {
            return errorType;
        }
    }

    enum QrDecodeErrorType {
        INVALID_COMPRESSION,
        UNSUPPORTED_VERSION,
        INVALID_FORMAT,
        SIGNATURE_FAILURE,
        UNKNOWN
    }
}
