package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class ImageUploadValidator {
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");

    public ValidatedImage validate(MultipartFile file, long maximumBytes) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Please select an image");
        if (file.getSize() > maximumBytes) throw new BadRequestException("Image exceeds the size limit");
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) throw new BadRequestException("Only JPEG, PNG, or WebP images are allowed");
        try {
            byte[] content = file.getBytes();
            Dimensions dimensions = dimensions(content, contentType);
            if (dimensions.width <= 0 || dimensions.height <= 0
                    || dimensions.width > MAX_DIMENSION || dimensions.height > MAX_DIMENSION
                    || (long) dimensions.width * dimensions.height > MAX_PIXELS) {
                throw new BadRequestException("Image dimensions must be at most 4096 x 4096 pixels");
            }
            return new ValidatedImage(content, contentType, extension);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the uploaded image");
        }
    }

    private Dimensions dimensions(byte[] bytes, String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> pngDimensions(bytes);
            case MediaType.IMAGE_JPEG_VALUE -> jpegDimensions(bytes);
            case "image/webp" -> webpDimensions(bytes);
            default -> throw invalidImage();
        };
    }

    private Dimensions pngDimensions(byte[] bytes) {
        if (bytes.length < 24 || unsigned(bytes[0]) != 0x89
                || bytes[1] != 'P' || bytes[2] != 'N' || bytes[3] != 'G') {
            throw invalidImage();
        }
        return new Dimensions(bigEndianInt(bytes, 16), bigEndianInt(bytes, 20));
    }

    private Dimensions jpegDimensions(byte[] bytes) {
        if (bytes.length < 4 || unsigned(bytes[0]) != 0xff || unsigned(bytes[1]) != 0xd8) {
            throw invalidImage();
        }
        int offset = 2;
        while (offset + 8 < bytes.length) {
            if (unsigned(bytes[offset]) != 0xff) {
                offset++;
                continue;
            }
            int marker = unsigned(bytes[offset + 1]);
            offset += 2;
            if (marker == 0xd8 || marker == 0xd9) continue;
            if (offset + 2 > bytes.length) break;
            int length = (unsigned(bytes[offset]) << 8) | unsigned(bytes[offset + 1]);
            if (length < 2 || offset + length > bytes.length) break;
            if (isStartOfFrame(marker) && length >= 7) {
                int height = (unsigned(bytes[offset + 3]) << 8) | unsigned(bytes[offset + 4]);
                int width = (unsigned(bytes[offset + 5]) << 8) | unsigned(bytes[offset + 6]);
                return new Dimensions(width, height);
            }
            offset += length;
        }
        throw invalidImage();
    }

    private Dimensions webpDimensions(byte[] bytes) {
        if (bytes.length < 30 || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F'
                || bytes[3] != 'F' || bytes[8] != 'W' || bytes[9] != 'E'
                || bytes[10] != 'B' || bytes[11] != 'P') {
            throw invalidImage();
        }
        String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if ("VP8X".equals(chunk)) {
            return new Dimensions(littleEndian24(bytes, 24) + 1, littleEndian24(bytes, 27) + 1);
        }
        if ("VP8L".equals(chunk) && unsigned(bytes[20]) == 0x2f) {
            int width = 1 + unsigned(bytes[21]) + ((unsigned(bytes[22]) & 0x3f) << 8);
            int height = 1 + ((unsigned(bytes[22]) & 0xc0) >> 6)
                    + (unsigned(bytes[23]) << 2) + ((unsigned(bytes[24]) & 0x0f) << 10);
            return new Dimensions(width, height);
        }
        if ("VP8 ".equals(chunk) && unsigned(bytes[23]) == 0x9d
                && unsigned(bytes[24]) == 0x01 && unsigned(bytes[25]) == 0x2a) {
            int width = (unsigned(bytes[26]) | unsigned(bytes[27]) << 8) & 0x3fff;
            int height = (unsigned(bytes[28]) | unsigned(bytes[29]) << 8) & 0x3fff;
            return new Dimensions(width, height);
        }
        throw invalidImage();
    }

    private boolean isStartOfFrame(int marker) {
        return marker >= 0xc0 && marker <= 0xcf
                && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

    private int bigEndianInt(byte[] bytes, int offset) {
        return unsigned(bytes[offset]) << 24 | unsigned(bytes[offset + 1]) << 16
                | unsigned(bytes[offset + 2]) << 8 | unsigned(bytes[offset + 3]);
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
                | unsigned(bytes[offset + 2]) << 16;
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private BadRequestException invalidImage() {
        return new BadRequestException("Uploaded file content is not a valid image");
    }

    private record Dimensions(int width, int height) {}

    public record ValidatedImage(byte[] content, String contentType, String extension) {}
}
