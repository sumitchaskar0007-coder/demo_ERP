package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadValidatorTest {
    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsValidPngWithinLimits() {
        byte[] png = pngHeader(320, 240);
        var file = new MockMultipartFile("file", "photo.png", "image/png", png);

        var result = validator.validate(file, 1024);

        assertEquals("image/png", result.contentType());
        assertEquals(".png", result.extension());
    }

    @Test
    void rejectsSpoofedContentAndOversizedDimensions() {
        var spoofed = new MockMultipartFile("file", "photo.png", "image/png", new byte[24]);
        assertThrows(BadRequestException.class, () -> validator.validate(spoofed, 1024));

        var huge = new MockMultipartFile("file", "photo.png", "image/png", pngHeader(5000, 10));
        assertThrows(BadRequestException.class, () -> validator.validate(huge, 1024));
    }

    @Test
    void rejectsFilesOverByteLimit() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", pngHeader(1, 1));
        assertThrows(BadRequestException.class, () -> validator.validate(file, 8));
    }

    private byte[] pngHeader(int width, int height) {
        byte[] bytes = new byte[24];
        bytes[0] = (byte) 0x89;
        bytes[1] = 'P';
        bytes[2] = 'N';
        bytes[3] = 'G';
        writeBigEndian(bytes, 16, width);
        writeBigEndian(bytes, 20, height);
        return bytes;
    }

    private void writeBigEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }
}
