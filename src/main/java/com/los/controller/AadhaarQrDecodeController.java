package com.los.controller;

import com.los.dto.runtime.AadhaarQrDecodeRequest;
import com.los.dto.runtime.AadhaarQrDecodeResponse;
import com.los.dto.runtime.ErrorResponse;
import com.los.service.AadhaarQrDecodeService;
import com.los.service.qr.UidaiSecureQrDecoder;
import com.los.util.CorrelationIdHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Aadhaar Secure QR decoding.
 * Backend-only Aadhaar handling - frontend sends Base64 payload, backend decodes.
 */
@RestController
@RequestMapping("/api/v1/qr/aadhaar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Aadhaar QR API", description = "Aadhaar Secure QR decoding endpoints")
public class AadhaarQrDecodeController {

    private final AadhaarQrDecodeService qrDecodeService;

    @Operation(summary = "Decode Aadhaar Secure QR from numeric string payload")
    @PostMapping("/decode")
    public ResponseEntity<?> decodeAadhaarQr(@Valid @RequestBody AadhaarQrDecodeRequest request) {
        String correlationId = CorrelationIdHolder.get();
        
        // Safe logging: length only, no content
        int payloadLength = request.getQrPayload() != null ? request.getQrPayload().length() : 0;
        
        log.info("[{}] Aadhaar QR decode request received (numeric string length: {})", 
                correlationId, payloadLength);

        try {
            AadhaarQrDecodeResponse response = qrDecodeService.decodeAadhaarQr(request.getQrPayload());
            log.info("[{}] Aadhaar QR decoded successfully", correlationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("[{}] Invalid request: {}", correlationId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.builder()
                            .errorCode("INVALID_REQUEST")
                            .message(e.getMessage())
                            .correlationId(correlationId)
                            .build());
        } catch (UidaiSecureQrDecoder.QrDecodeException e) {
            log.error("[{}] QR decode failure: {} (error type: {})", 
                    correlationId, e.getMessage(), e.getErrorType());
            
            // Return specific error message based on error type
            String errorMessage = "Invalid or unsupported Aadhaar QR";
            if (e.getErrorType() == UidaiSecureQrDecoder.QrDecodeErrorType.INVALID_FORMAT 
                    && e.getMessage() != null && e.getMessage().contains("Unsupported")) {
                errorMessage = "Unsupported Aadhaar QR format";
            } else if (e.getErrorType() == UidaiSecureQrDecoder.QrDecodeErrorType.INVALID_COMPRESSION) {
                errorMessage = "Unsupported Aadhaar QR compression format";
            }
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ErrorResponse.builder()
                            .errorCode("QR_DECODE_FAILED")
                            .message(errorMessage)
                            .correlationId(correlationId)
                            .build());
        } catch (Exception e) {
            log.error("[{}] Unexpected error during QR decode", correlationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.builder()
                            .errorCode("INTERNAL_ERROR")
                            .message("An unexpected error occurred")
                            .correlationId(correlationId)
                            .build());
        }
    }
}
