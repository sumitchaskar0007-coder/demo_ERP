package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.storage.ObjectStorageService;
import com.collegeerp.erp.storage.SecureFileContentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentProofStorageService {
    private static final Logger log = LoggerFactory.getLogger(PaymentProofStorageService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final ObjectStorageService storage;
    private final SecureFileContentValidator fileValidator;

    public PaymentProofStorageService(ObjectStorageService storage) {
        this(storage, new SecureFileContentValidator());
    }

    @Autowired
    public PaymentProofStorageService(
            ObjectStorageService storage, SecureFileContentValidator fileValidator) {
        this.storage = storage;
        this.fileValidator = fileValidator;
    }

    public String save(MultipartFile file) {
        SecureFileContentValidator.ValidatedContent validated =
                fileValidator.validateDocument(file, MAX_BYTES);
        String contentType = validated.contentType();
        String extension = validated.extension();
        Long collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        Long userId = SecurityUtils.getCurrentUserId();
        if (collegeId == null) throw new BadRequestException("A college is required for payment proof storage");
        String storageName = "colleges/" + collegeId
                + "/students/" + userId
                + "/fee-payment-proofs/" + UUID.randomUUID() + extension;
        try {
            storage.put(storageName, validated.content(), contentType);
            return storageName;
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

    private String extension(String storageName) {
        if (storageName == null) return "";
        int dot = storageName.lastIndexOf('.');
        return dot < 0 ? "" : storageName.substring(dot).toLowerCase(Locale.ROOT);
    }

    public record PaymentProofResource(Resource resource, MediaType mediaType) {}
}
