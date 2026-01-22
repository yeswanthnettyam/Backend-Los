package com.los.service.qr.impl;

import com.los.service.qr.UidaiSecureQrDecoder;
import com.los.util.CorrelationIdHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
@Slf4j
public class UidaiSecureQrDecoderImpl implements UidaiSecureQrDecoder {

    private static final byte SEPARATOR_BYTE = (byte) 255;

    // Indexes (same as Android)
    private static final int EMAIL_MOBILE_FLAG_INDEX = 0;
    private static final int REFERENCE_ID_INDEX = 1;
    private static final int NAME_INDEX = 2;
    private static final int DOB_INDEX = 3;
    private static final int GENDER_INDEX = 4;
    private static final int CARE_OF_INDEX = 5;
    private static final int DISTRICT_INDEX = 6;
    private static final int LANDMARK_INDEX = 7;
    private static final int HOUSE_INDEX = 8;
    private static final int LOCATION_INDEX = 9;
    private static final int PIN_CODE_INDEX = 10;
    private static final int POST_OFFICE_INDEX = 11;
    private static final int STATE_INDEX = 12;
    private static final int STREET_INDEX = 13;
    private static final int SUB_DISTRICT_INDEX = 14;
    private static final int VTC_INDEX = 15;

    private static final int BUFFER_SIZE = 4096;
    private static final int MAX_DECOMPRESSED_SIZE = 10 * 1024 * 1024;

    @Override
    public DecodedAadhaarData decode(byte[] qrBytes) throws QrDecodeException {
        String cid = CorrelationIdHolder.get();

        String numeric = new String(qrBytes, StandardCharsets.US_ASCII);
        if (!numeric.matches("\\d+")) {
            throw new QrDecodeException("Invalid numeric QR", QrDecodeErrorType.INVALID_FORMAT);
        }

        BigInteger bigInt = new BigInteger(numeric, 10);
        byte[] compressed = bigInt.toByteArray();

        byte[] decompressed = decompressData(compressed, cid);

        List<byte[]> parts = separateData(decompressed);
        if (parts.size() < VTC_INDEX + 1) {
            throw new QrDecodeException("Incomplete QR payload", QrDecodeErrorType.INVALID_FORMAT);
        }

        List<String> decoded = decodeStrings(parts);

        DecodedAadhaarData.DecodedAadhaarDataBuilder builder =
                DecodedAadhaarData.builder();

        builder
                .name(decoded.get(NAME_INDEX))
                .dob(decoded.get(DOB_INDEX))
                .gender(decoded.get(GENDER_INDEX))
                .aadhaarNumber(decoded.get(REFERENCE_ID_INDEX)) // last 4 digits only
                .careOf(decoded.get(CARE_OF_INDEX))
                .district(decoded.get(DISTRICT_INDEX))
                .landmark(decoded.get(LANDMARK_INDEX))
                .house(decoded.get(HOUSE_INDEX))
                .location(decoded.get(LOCATION_INDEX))
                .pinCode(decoded.get(PIN_CODE_INDEX))
                .postOffice(decoded.get(POST_OFFICE_INDEX))
                .state(decoded.get(STATE_INDEX))
                .street(decoded.get(STREET_INDEX))
                .subDistrict(decoded.get(SUB_DISTRICT_INDEX))
                .vtc(decoded.get(VTC_INDEX));

        int emailMobileFlag = Integer.parseInt(decoded.get(EMAIL_MOBILE_FLAG_INDEX));

        extractEmailMobileAndSignature(decompressed, emailMobileFlag, builder);

        log.info("[{}] Aadhaar QR decoded successfully (backend-safe)", cid);
        return builder.build();
    }

    /* =========================
       GZIP DECOMPRESSION
       ========================= */

    private byte[] decompressData(byte[] input, String cid) throws QrDecodeException {
        try (ByteArrayInputStream bin = new ByteArrayInputStream(input);
             GZIPInputStream gis = new GZIPInputStream(bin);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = gis.read(buf)) != -1) {
                bos.write(buf, 0, read);
                if (bos.size() > MAX_DECOMPRESSED_SIZE) {
                    throw new QrDecodeException("QR payload too large",
                            QrDecodeErrorType.INVALID_COMPRESSION);
                }
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new QrDecodeException("GZIP decompression failed",
                    QrDecodeErrorType.INVALID_COMPRESSION, e);
        }
    }

    /* =========================
       DELIMITER SPLIT
       ========================= */

    private List<byte[]> separateData(byte[] source) {
        List<byte[]> parts = new ArrayList<>();
        int begin = 0;

        for (int i = 0; i < source.length; i++) {
            if (source[i] == SEPARATOR_BYTE) {
                if (i != 0 && i != source.length - 1) {
                    parts.add(Arrays.copyOfRange(source, begin, i));
                }
                begin = i + 1;
                if (parts.size() == VTC_INDEX + 1) {
                    break;
                }
            }
        }
        return parts;
    }

    /* =========================
       STRING DECODE
       ========================= */

    private List<String> decodeStrings(List<byte[]> parts) {
        List<String> decoded = new ArrayList<>();
        for (byte[] part : parts) {
            decoded.add(new String(part, StandardCharsets.ISO_8859_1));
        }
        return decoded;
    }

    /* =========================
       EMAIL / MOBILE / SIGNATURE
       ========================= */

    private void extractEmailMobileAndSignature(byte[] data,
                                                int flag,
                                                DecodedAadhaarData.DecodedAadhaarDataBuilder builder) {

        int imageEndIndex;

        switch (flag) {
            case 3: // email + mobile
                builder.mobileHash(bytesToHex(slice(data, data.length - 289, data.length - 257)));
                builder.emailHash(bytesToHex(slice(data, data.length - 322, data.length - 290)));
                imageEndIndex = data.length - 323;
                break;

            case 2: // mobile only
                builder.mobileHash(bytesToHex(slice(data, data.length - 289, data.length - 257)));
                imageEndIndex = data.length - 290;
                break;

            case 1: // email only
                builder.emailHash(bytesToHex(slice(data, data.length - 289, data.length - 257)));
                imageEndIndex = data.length - 290;
                break;

            default:
                imageEndIndex = data.length - 257;
        }

        // Signature (last 256 bytes)
        builder.signature(
                new String(slice(data, data.length - 257, data.length - 1),
                        StandardCharsets.ISO_8859_1)
        );
    }

    private byte[] slice(byte[] src, int start, int end) {
        return Arrays.copyOfRange(src, start, end + 1);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] table = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = table[v >>> 4];
            hex[i * 2 + 1] = table[v & 0x0F];
        }
        return new String(hex);
    }
}

