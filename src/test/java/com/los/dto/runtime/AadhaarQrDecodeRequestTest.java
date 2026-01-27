package com.los.dto.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AadhaarQrDecodeRequest DTO.
 * Tests validation and basic functionality.
 */
class AadhaarQrDecodeRequestTest {

    private static final String VALID_NUMERIC_PAYLOAD = "1".repeat(1001); // Minimum valid length

    @Test
    void testGetQrPayload_ReturnsValue() {
        // Given: Valid payload
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(VALID_NUMERIC_PAYLOAD)
                .build();

        // When & Then: Should return the payload
        assertEquals(VALID_NUMERIC_PAYLOAD, request.getQrPayload());
    }

    @Test
    void testGetQrPayload_ReturnsNullWhenNotSet() {
        // Given: Payload not set
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .build();

        // When & Then: Should return null
        assertNull(request.getQrPayload());
    }

    @Test
    void testGetQrPayload_ReturnsEmptyWhenSetToEmpty() {
        // Given: Empty payload
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload("")
                .build();

        // When & Then: Should return empty string
        assertEquals("", request.getQrPayload());
    }

    @Test
    void testBuilder_CanSetQrPayload() {
        // Given: Builder with payload
        String payload = VALID_NUMERIC_PAYLOAD;
        
        // When: Building request
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .qrPayload(payload)
                .build();

        // Then: Should have the payload
        assertEquals(payload, request.getQrPayload());
    }

    @Test
    void testBuilder_CanBuildEmptyRequest() {
        // Given: Builder without payload
        // When: Building request
        AadhaarQrDecodeRequest request = AadhaarQrDecodeRequest.builder()
                .build();

        // Then: Should have null payload
        assertNull(request.getQrPayload());
    }

    @Test
    void testAllArgsConstructor() {
        // Given: Payload value
        String payload = VALID_NUMERIC_PAYLOAD;
        
        // When: Using all args constructor
        AadhaarQrDecodeRequest request = new AadhaarQrDecodeRequest(payload);

        // Then: Should have the payload
        assertEquals(payload, request.getQrPayload());
    }

    @Test
    void testNoArgsConstructor() {
        // When: Using no args constructor
        AadhaarQrDecodeRequest request = new AadhaarQrDecodeRequest();

        // Then: Should have null payload
        assertNull(request.getQrPayload());
    }

    @Test
    void testSetter() {
        // Given: Request object
        AadhaarQrDecodeRequest request = new AadhaarQrDecodeRequest();
        String payload = VALID_NUMERIC_PAYLOAD;

        // When: Setting payload
        request.setQrPayload(payload);

        // Then: Should have the payload
        assertEquals(payload, request.getQrPayload());
    }
}
