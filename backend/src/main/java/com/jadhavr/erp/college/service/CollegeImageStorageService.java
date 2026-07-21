package com.jadhavr.erp.college.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.storage.ImageUploadValidator;
import com.jadhavr.erp.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CollegeImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(CollegeImageStorageService.class);
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Pattern PENDING_REFERENCE = Pattern.compile(
            "^/api/super-admin/colleges/images/pending/(logo|qr-code)/([0-9a-f-]{36})(\\.(?:jpg|png|webp))$");

    private final ObjectStorageService storage;
    private final ImageUploadValidator images;

    public CollegeImageStorageService(ObjectStorageService storage, ImageUploadValidator images) {
        this.storage = storage;
        this.images = images;
    }

    public String storePending(MultipartFile file, String kind) {
        requireKind(kind);
        ImageUploadValidator.ValidatedImage image = images.validate(file, MAX_SIZE);
        String id = UUID.randomUUID().toString();
        String key = "pending/college-assets/" + kind + "/" + id + image.extension();
        storage.put(key, image.content(), image.contentType());
        return "/api/super-admin/colleges/images/pending/" + kind + "/" + id + image.extension();
    }

    public String claim(String reference, Long collegeId, String kind, String oldKey) {
        if (reference == null || reference.isBlank()) {
            if (oldKey != null) registerDeleteAfterCommit(oldKey);
            return null;
        }
        requireKind(kind);
        Matcher matcher = PENDING_REFERENCE.matcher(reference);
        if (!matcher.matches() || !kind.equals(matcher.group(1))) {
            if (reference.equals(responseUrl(collegeId, kind)) && oldKey != null) return oldKey;
            throw new BadRequestException("Upload the college image before saving");
        }
        String pendingKey = "pending/college-assets/" + kind + "/" + matcher.group(2) + matcher.group(3);
        ObjectStorageService.StoredObject object = storage.get(pendingKey);
        String newKey = "colleges/" + collegeId + "/branding/" + kind + "/"
                + matcher.group(2) + matcher.group(3);
        storage.put(newKey, object.content(), object.contentType());
        registerCleanup(newKey, pendingKey, oldKey);
        return newKey;
    }

    public ImageResource load(String key) {
        if (key == null) throw new com.jadhavr.erp.common.exception.ResourceNotFoundException("Image not found");
        ObjectStorageService.StoredObject object = storage.get(key);
        return new ImageResource(new ByteArrayResource(object.content()),
                MediaType.parseMediaType(object.contentType()));
    }

    public ImageResource loadPending(String kind, String id, String extension) {
        requireKind(kind);
        if (!id.matches("[0-9a-f-]{36}") || !extension.matches("jpg|png|webp")) {
            throw new BadRequestException("Invalid image reference");
        }
        return load("pending/college-assets/" + kind + "/" + id + "." + extension);
    }

    public String responseUrl(Long collegeId, String kind) {
        return "/api/super-admin/colleges/" + collegeId + "/images/" + kind;
    }

    public String publicLogoUrl(String collegeCode, String key) {
        return key == null ? null : "/api/public/admissions/college/" + collegeCode + "/logo";
    }

    private void registerCleanup(String newKey, String pendingKey, String oldKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(pendingKey);
            if (oldKey != null) safeDelete(oldKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(pendingKey);
                if (oldKey != null) safeDelete(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(newKey);
            }
        });
    }

    private void registerDeleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(key);
            }
        });
    }

    private void requireKind(String kind) {
        if (!"logo".equals(kind) && !"qr-code".equals(kind)) {
            throw new BadRequestException("Image type must be logo or qr-code");
        }
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Unable to complete college-image storage cleanup", exception);
        }
    }

    public record ImageResource(ByteArrayResource resource, MediaType mediaType) {}
}
