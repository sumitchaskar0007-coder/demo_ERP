package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.common.exception.BadRequestException;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class AdmissionDocumentUploadPolicy {
    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, Set.of(".pdf"),
            MediaType.IMAGE_JPEG_VALUE, Set.of(".jpg", ".jpeg"),
            MediaType.IMAGE_PNG_VALUE, Set.of(".png"));
    private static final Map<String, String> CANONICAL_EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png");

    private AdmissionDocumentUploadPolicy() {}

    static ValidatedUpload validate(
            String originalFilename,
            String contentType,
            long fileSize,
            String sha256,
            long maxBytes) {
        if (fileSize <= 0) throw new BadRequestException("Document must not be empty");
        if (fileSize > maxBytes) {
            throw new BadRequestException("Document must not exceed " + maxBytes + " bytes");
        }

        String normalizedContentType = contentType == null
                ? ""
                : contentType.trim().toLowerCase(Locale.ROOT);
        Set<String> extensions = ALLOWED_EXTENSIONS.get(normalizedContentType);
        if (extensions == null) {
            throw new BadRequestException("Only PDF, JPEG, or PNG documents are allowed");
        }

        String filename = validateFilename(originalFilename);
        String lowercaseFilename = filename.toLowerCase(Locale.ROOT);
        if (extensions.stream().noneMatch(lowercaseFilename::endsWith)) {
            throw new BadRequestException("Document filename extension does not match its content type");
        }

        if (sha256 == null || !SHA256_HEX.matcher(sha256).matches()) {
            throw new BadRequestException("A valid SHA-256 checksum is required");
        }
        String normalizedSha256 = sha256.toLowerCase(Locale.ROOT);
        String checksumBase64 = Base64.getEncoder().encodeToString(
                HexFormat.of().parseHex(normalizedSha256));
        return new ValidatedUpload(
                filename,
                normalizedContentType,
                fileSize,
                normalizedSha256,
                checksumBase64,
                CANONICAL_EXTENSIONS.get(normalizedContentType));
    }

    static void validateMaxBytes(long maxBytes) {
        long databaseMaximum = 5L * 1024 * 1024;
        if (maxBytes <= 0 || maxBytes > databaseMaximum) {
            throw new IllegalArgumentException(
                    "app.storage.presign.max-bytes must be between 1 and " + databaseMaximum);
        }
    }

    private static String validateFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Document filename is required");
        }
        String filename = Normalizer.normalize(originalFilename.trim(), Normalizer.Form.NFC);
        if (filename.isBlank() || filename.length() > 255
                || filename.equals(".") || filename.equals("..")
                || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0
                || filename.getBytes(StandardCharsets.UTF_8).length > 1024
                || filename.codePoints().anyMatch(AdmissionDocumentUploadPolicy::isUnsafeCodePoint)) {
            throw new BadRequestException("Document filename is invalid");
        }
        return filename;
    }

    private static boolean isUnsafeCodePoint(int codePoint) {
        return Character.isISOControl(codePoint)
                || Character.getType(codePoint) == Character.FORMAT;
    }

    record ValidatedUpload(
            String originalFilename,
            String contentType,
            long fileSize,
            String sha256,
            String checksumBase64,
            String storageExtension) {
    }
}
