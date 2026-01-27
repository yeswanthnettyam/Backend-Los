package com.los.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.los.dto.runtime.AadhaarQrDecodeRequest;
import com.los.dto.runtime.AadhaarQrDecodeResponse;
import com.los.service.AadhaarQrDecodeService;
import com.los.service.qr.UidaiSecureQrDecoder;
import com.los.util.CorrelationIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Aadhaar QR decode controller.
 * Tests validation and error handling.
 */
@WebMvcTest(AadhaarQrDecodeController.class)
class AadhaarQrDecodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AadhaarQrDecodeService qrDecodeService;

    private static final String VALID_NUMERIC_PAYLOAD = "1".repeat(1001); // Minimum valid length (1001 digits)
    private static final String DECODED_NAME = "John Doe";
    private static final String DECODED_GENDER = "M";
    private static final String DECODED_DOB = "1990-01-15";
    private static final String DECODED_AADHAAR_LAST4 = "1234";

    @BeforeEach
    void setUp() {
        CorrelationIdHolder.set("test-correlation-id");
    }

    @AfterEach
    void tearDown() {
        CorrelationIdHolder.clear();
    }

    @Test
    void testDecodeWithValidPayload() throws Exception {
        // Given: Request with valid numeric payload
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(VALID_NUMERIC_PAYLOAD)
                .build();

        AadhaarQrDecodeResponse response = AadhaarQrDecodeResponse.builder()
                .name(DECODED_NAME)
                .gender(DECODED_GENDER)
                .dob(DECODED_DOB)
                .aadhaarLast4(DECODED_AADHAAR_LAST4)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_NUMERIC_PAYLOAD)).thenReturn(response);

        // When & Then: Should decode successfully
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DECODED_NAME))
                .andExpect(jsonPath("$.gender").value(DECODED_GENDER))
                .andExpect(jsonPath("$.dob").value(DECODED_DOB))
                .andExpect(jsonPath("$.aadhaarLast4").value(DECODED_AADHAAR_LAST4));
    }

    @Test
    void testDecodeWithMissingPayload() throws Exception {
        // Given: Request with missing payload
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .build();

        // When & Then: Should return 400 with validation error
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDecodeWithEmptyPayload() throws Exception {
        // Given: Request with empty payload
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload("")
                .build();

        // When & Then: Should return 400 with validation error
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDecodeWithNonNumericPayload() throws Exception {
        // Given: Request with non-numeric payload
        String nonNumericPayload = "abc".repeat(400); // 1200 characters but not numeric
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(nonNumericPayload)
                .build();

        // When & Then: Should return 400 with validation error
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDecodeWithTooShortPayload() throws Exception {
        // Given: Request with payload shorter than minimum (1001 characters)
        String shortPayload = "1".repeat(1000); // 1000 digits (below minimum)
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(shortPayload)
                .build();

        // When & Then: Should return 400 with validation error
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDecodeWithServiceException() throws Exception {
        // Given: Request with valid payload but service throws exception
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(VALID_NUMERIC_PAYLOAD)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_NUMERIC_PAYLOAD))
                .thenThrow(new IllegalArgumentException("QR payload is required"));

        // When & Then: Should return 400 with error response
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("QR payload is required"));
    }

    @Test
    void testDecodeWithQrDecodeException() throws Exception {
        // Given: Request with valid payload but QR decode fails
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(VALID_NUMERIC_PAYLOAD)
                .build();

        UidaiSecureQrDecoder.QrDecodeException decodeException = 
                new UidaiSecureQrDecoder.QrDecodeException(
                        "Invalid QR format", 
                        UidaiSecureQrDecoder.QrDecodeErrorType.INVALID_FORMAT);

        when(qrDecodeService.decodeAadhaarQr(VALID_NUMERIC_PAYLOAD))
                .thenThrow(decodeException);

        // When & Then: Should return 422 with error response
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("QR_DECODE_FAILED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testDecodeWithUnexpectedException() throws Exception {
        // Given: Request with valid payload but unexpected exception occurs
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(VALID_NUMERIC_PAYLOAD)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_NUMERIC_PAYLOAD))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then: Should return 500 with error response
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
