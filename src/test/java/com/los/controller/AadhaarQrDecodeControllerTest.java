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
 * Tests backward compatibility with both qrPayloadBase64 and qrData fields.
 */
@WebMvcTest(AadhaarQrDecodeController.class)
class AadhaarQrDecodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AadhaarQrDecodeService qrDecodeService;

    private static final String VALID_BASE64 = "dGVzdHFycGF5bG9hZA=="; // "testqrpayload" in Base64
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
    void testDecodeWithQrPayloadBase64() throws Exception {
        // Given: Request with qrPayloadBase64 field
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64(VALID_BASE64)
                .build();

        AadhaarQrDecodeResponse response = AadhaarQrDecodeResponse.builder()
                .name(DECODED_NAME)
                .gender(DECODED_GENDER)
                .dob(DECODED_DOB)
                .aadhaarLast4(DECODED_AADHAAR_LAST4)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_BASE64)).thenReturn(response);

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
    void testDecodeWithQrDataFallback() throws Exception {
        // Given: Request with qrData field (backward compatibility)
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrData(VALID_BASE64)
                .build();

        AadhaarQrDecodeResponse response = AadhaarQrDecodeResponse.builder()
                .name(DECODED_NAME)
                .gender(DECODED_GENDER)
                .dob(DECODED_DOB)
                .aadhaarLast4(DECODED_AADHAAR_LAST4)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_BASE64)).thenReturn(response);

        // When & Then: Should decode successfully using qrData
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
    void testDecodeWithBothFields_PrefersQrPayloadBase64() throws Exception {
        // Given: Request with both fields (should prefer qrPayloadBase64)
        String primaryPayload = VALID_BASE64;
        String fallbackPayload = "YWx0ZXJuYXRpdmU="; // "alternative" in Base64

        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64(primaryPayload)
                .qrData(fallbackPayload)
                .build();

        AadhaarQrDecodeResponse response = AadhaarQrDecodeResponse.builder()
                .name(DECODED_NAME)
                .gender(DECODED_GENDER)
                .dob(DECODED_DOB)
                .aadhaarLast4(DECODED_AADHAAR_LAST4)
                .build();

        when(qrDecodeService.decodeAadhaarQr(primaryPayload)).thenReturn(response);

        // When & Then: Should use qrPayloadBase64 (primary field)
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DECODED_NAME));
    }

    @Test
    void testDecodeWithEmptyQrPayloadBase64_UsesQrData() throws Exception {
        // Given: Request with empty qrPayloadBase64 but valid qrData
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("")
                .qrData(VALID_BASE64)
                .build();

        AadhaarQrDecodeResponse response = AadhaarQrDecodeResponse.builder()
                .name(DECODED_NAME)
                .gender(DECODED_GENDER)
                .dob(DECODED_DOB)
                .aadhaarLast4(DECODED_AADHAAR_LAST4)
                .build();

        when(qrDecodeService.decodeAadhaarQr(VALID_BASE64)).thenReturn(response);

        // When & Then: Should use qrData as fallback
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DECODED_NAME));
    }

    @Test
    void testDecodeWithBothFieldsMissing() throws Exception {
        // Given: Request with both fields missing
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .build();

        // When & Then: Should return 400 with error message
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("QR payload Base64 is required"));
    }

    @Test
    void testDecodeWithBothFieldsEmpty() throws Exception {
        // Given: Request with both fields empty
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("")
                .qrData("")
                .build();

        // When & Then: Should return 400 with error message
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("QR payload Base64 is required"));
    }

    @Test
    void testDecodeWithWhitespaceOnly() throws Exception {
        // Given: Request with whitespace-only fields
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("   ")
                .qrData("   ")
                .build();

        // When & Then: Should return 400 with error message
        mockMvc.perform(post("/api/v1/qr/aadhaar/decode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("QR payload Base64 is required"));
    }
}
