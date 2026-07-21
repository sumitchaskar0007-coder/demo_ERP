package com.jadhavr.erp.auth.service;

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

@Service
public class ProfileImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(ProfileImageStorageService.class);
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final ObjectStorageService storage;
    private final ImageUploadValidator images;

    public ProfileImageStorageService(ObjectStorageService storage, ImageUploadValidator images) {
        this.storage = storage;
        this.images = images;
    }

    public String store(MultipartFile file, Long userId, Long collegeId) {
        ImageUploadValidator.ValidatedImage image = images.validate(file, MAX_SIZE);
        String tenant = collegeId == null ? "platform" : "colleges/" + collegeId;
        String key = tenant + "/users/" + userId + "/profile/"
                + UUID.randomUUID() + image.extension();
        storage.put(key, image.content(), image.contentType());
        return key;
    }

    public ImageResource load(String key) {
        ObjectStorageService.StoredObject object = storage.get(key);
        return new ImageResource(new ByteArrayResource(object.content()),
                MediaType.parseMediaType(object.contentType()));
    }

    public void cleanupAfterTransaction(String newKey, String oldKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (oldKey != null) safeDelete(oldKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldKey != null) safeDelete(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(newKey);
            }
        });
    }

    public void deleteNewObjectAfterFailure(String key) {
        safeDelete(key);
    }

    public void deleteAfterCommit(String key) {
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

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Unable to complete profile-image storage cleanup", exception);
        }
    }

    public record ImageResource(ByteArrayResource resource, MediaType mediaType) {}
}
