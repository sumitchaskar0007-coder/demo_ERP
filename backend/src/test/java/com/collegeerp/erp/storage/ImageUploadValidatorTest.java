package com.collegeerp.erp.storage;

import com.collegeerp.erp.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUploadValidatorTest {
    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsValidPngWithinLimits() throws IOException {
        byte[] png = validImage("png", 320, 240);
        var file = new MockMultipartFile("file", "photo.png", "image/png", png);

        var result = validator.validate(file, 1024);

        assertEquals("image/png", result.contentType());
        assertEquals(".png", result.extension());
        assertTrue(result.content().length > 0);
        assertTrue(ImageIO.read(new java.io.ByteArrayInputStream(result.content())) != null);
    }

    @Test
    void rejectsSpoofedContentAndOversizedDimensions() throws IOException {
        var spoofed = new MockMultipartFile("file", "photo.png", "image/png", new byte[24]);
        assertThrows(BadRequestException.class, () -> validator.validate(spoofed, 1024));

        byte[] oversized = validImage("png", 4097, 1);
        var huge = new MockMultipartFile("file", "photo.png", "image/png", oversized);
        assertThrows(BadRequestException.class, () -> validator.validate(huge, 100_000));
    }

    @Test
    void rejectsFilesOverByteLimit() throws IOException {
        var file = new MockMultipartFile("file", "photo.png", "image/png", validImage("png", 1, 1));
        assertThrows(BadRequestException.class, () -> validator.validate(file, 8));
    }

    @Test
    void reencodesImagesAndRemovesTrailingPolyglotContent() throws IOException {
        byte[] image = validImage("png", 8, 8);
        byte[] marker = "<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] polyglot = Arrays.copyOf(image, image.length + marker.length);
        System.arraycopy(marker, 0, polyglot, image.length, marker.length);

        var result = validator.validate(
                new MockMultipartFile("file", "photo.png", "image/png", polyglot),
                10_000);

        assertTrue(result.content().length < polyglot.length);
        assertFalse(new String(result.content(), java.nio.charset.StandardCharsets.ISO_8859_1)
                .contains("<script>"));
    }

    @Test
    void rejectsWebpImages() {
        var file = new MockMultipartFile("file", "photo.webp", "image/webp", new byte[32]);
        assertThrows(BadRequestException.class, () -> validator.validate(file, 1024));
    }

    private byte[] validImage(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return output.toByteArray();
    }
}
