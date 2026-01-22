package com.los.exception;

/**
 * Exception thrown when Aadhaar QR decoding fails.
 * Wraps UIDAI decoder exceptions for consistent error handling.
 */
public class QrDecodeException extends RuntimeException {

    public QrDecodeException(String message) {
        super(message);
    }

    public QrDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
