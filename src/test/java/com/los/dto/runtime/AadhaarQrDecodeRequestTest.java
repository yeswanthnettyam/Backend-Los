package com.los.dto.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AadhaarQrDecodeRequest DTO.
 * Tests backward compatibility logic for qrPayloadBase64 and qrData fields.
 */
class AadhaarQrDecodeRequestTest {

    @Test
    void testGetQrPayload_PrefersQrPayloadBase64() {
        // Given: Both fields present
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("primary")
                .qrData("fallback")
                .build();

        // When & Then: Should return primary field
        assertEquals("primary", request.getQrPayload());
        assertEquals("qrPayloadBase64", request.getUsedFieldName());
    }

    @Test
    void testGetQrPayload_UsesQrDataWhenPrimaryMissing() {
        // Given: Only qrData present
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrData("fallback")
                .build();

        // When & Then: Should return fallback field
        assertEquals("fallback", request.getQrPayload());
        assertEquals("qrData", request.getUsedFieldName());
    }

    @Test
    void testGetQrPayload_UsesQrDataWhenPrimaryEmpty() {
        // Given: Primary empty, fallback present
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("")
                .qrData("fallback")
                .build();

        // When & Then: Should return fallback field
        assertEquals("fallback", request.getQrPayload());
        assertEquals("qrData", request.getUsedFieldName());
    }

    @Test
    void testGetQrPayload_TrimsWhitespace() {
        // Given: Fields with whitespace
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("  payload  ")
                .build();

        // When & Then: Should return trimmed value
        assertEquals("payload", request.getQrPayload());
    }

    @Test
    void testHasQrPayload_ReturnsTrueWhenPresent() {
        // Given: QR payload present
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("payload")
                .build();

        // When & Then: Should return true
        assertTrue(request.hasQrPayload());
    }

    @Test
    void testHasQrPayload_ReturnsFalseWhenBothMissing() {
        // Given: Both fields missing
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .build();

        // When & Then: Should return false
        assertFalse(request.hasQrPayload());
        assertNull(request.getQrPayload());
        assertNull(request.getUsedFieldName());
    }

    @Test
    void testHasQrPayload_ReturnsFalseWhenBothEmpty() {
        // Given: Both fields empty
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("")
                .qrData("")
                .build();

        // When & Then: Should return false
        assertFalse(request.hasQrPayload());
        assertNull(request.getQrPayload());
        assertNull(request.getUsedFieldName());
    }

    @Test
    void testHasQrPayload_ReturnsFalseWhenBothWhitespace() {
        // Given: Both fields whitespace-only
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("   ")
                .qrData("   ")
                .build();

        // When & Then: Should return false
        assertFalse(request.hasQrPayload());
        assertNull(request.getQrPayload());
        assertNull(request.getUsedFieldName());
    }

    @Test
    void testGetUsedFieldName_ReturnsCorrectField() {
        // Test primary field
        AadhaarQrDecodeRequest request1 = AadhaarQrDecodeRequest.builder()
                .qrPayloadBase64("payload")
                .build();
        assertEquals("qrPayloadBase64", request1.getUsedFieldName());

        // Test fallback field
        AadhaarQrDecodeRequest request2 = AadhaarQrDecodeRequest.builder()
                .qrData("payload")
                .build();
        assertEquals("qrData", request2.getUsedFieldName());

        // Test both missing
        AadhaarQrDecodeRequest request3 = AadhaarQrDecodeRequest.builder()
                .build();
        assertNull(request3.getUsedFieldName());
    }
}
