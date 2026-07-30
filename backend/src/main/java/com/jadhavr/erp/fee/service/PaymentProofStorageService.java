package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentProofStorageService {
    private static final Logger log = LoggerFactory.getLogger(PaymentProofStorageService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");
    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(
            ".pdf", MediaType.APPLICATION_PDF_VALUE,
            ".jpg", MediaType.IMAGE_JPEG_VALUE,
            ".jpeg", MediaType.IMAGE_JPEG_VALUE,
            ".png", MediaType.IMAGE_PNG_VALUE,
            ".webp", "image/webp");
    private final ObjectStorageService storage;

    public PaymentProofStorageService(ObjectStorageService storage) {
        this.storage = storage;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Payment proof file is required");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Payment proof must not exceed 5 MB");
        String contentType = resolveContentType(file);
        String extension = EXTENSIONS.get(contentType);
        verifySignature(file, contentType);
        Long collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (collegeId == null) throw new BadRequestException("A college is required for payment proof storage");
        String storageName = "colleges/" + collegeId
                + "/students/" + userId
                + "/fee-payment-proofs/" + UUID.randomUUID() + extension;
        try {
            storage.put(storageName, file.getBytes(), contentType);
            return storageName;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the payment proof");
        } catch (RuntimeException exception) {
            safeDelete(storageName);
            throw exception;
        }
    }

    public PaymentProofResource load(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            throw new ResourceNotFoundException("Payment proof file not found");
        }
        ObjectStorageService.StoredObject object = storage.get(storageName);
        String contentType = object.contentType();
        if (contentType == null || contentType.isBlank()
                || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            contentType = switch (extension(storageName)) {
                case ".pdf" -> MediaType.APPLICATION_PDF_VALUE;
                case ".jpg" -> MediaType.IMAGE_JPEG_VALUE;
                case ".png" -> MediaType.IMAGE_PNG_VALUE;
                case ".webp" -> "image/webp";
                default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
            };
        }
        return new PaymentProofResource(
                new ByteArrayResource(object.content()),
                MediaType.parseMediaType(contentType));
    }

    public void delete(String storageName) {
        if (storageName == null || storageName.isBlank()) return;
        safeDelete(storageName);
    }

    private void safeDelete(String storageName) {
        try {
            storage.delete(storageName);
        } catch (RuntimeException exception) {
            log.warn("Unable to complete payment-proof storage cleanup", exception);
        }
    }

    private void verifySignature(MultipartFile file, String contentType) {
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            boolean valid = switch (contentType) {
                case MediaType.APPLICATION_PDF_VALUE -> header.length >= 5
                        && header[0] == '%' && header[1] == 'P' && header[2] == 'D'
                        && header[3] == 'F' && header[4] == '-';
                case MediaType.IMAGE_JPEG_VALUE -> header.length >= 3
                        && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                        && (header[2] & 0xff) == 0xff;
                case MediaType.IMAGE_PNG_VALUE -> header.length >= 8
                        && (header[0] & 0xff) == 0x89 && header[1] == 'P'
                        && header[2] == 'N' && header[3] == 'G';
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BadRequestException("Payment proof content does not match its file type");
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the payment proof");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String declaredType = file.getContentType();
        if (declaredType != null) {
            String normalized = declaredType.toLowerCase(Locale.ROOT).trim();
            if ("image/jpg".equals(normalized)) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            if (EXTENSIONS.containsKey(normalized)) {
                return normalized;
            }
            if (!normalized.isBlank()
                    && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(normalized)) {
                throw unsupportedType();
            }
        }

        String inferredType = CONTENT_TYPES_BY_EXTENSION.get(extension(file.getOriginalFilename()));
        if (inferredType == null) {
            throw unsupportedType();
        }
        return inferredType;
    }

    private BadRequestException unsupportedType() {
        return new BadRequestException("Only PDF, JPEG, PNG, or WebP payment proofs are allowed");
    }

    private String extension(String storageName) {
        if (storageName == null) return "";
        int dot = storageName.lastIndexOf('.');
        return dot < 0 ? "" : storageName.substring(dot).toLowerCase(Locale.ROOT);
    }

    public record PaymentProofResource(Resource resource, MediaType mediaType) {}
}
