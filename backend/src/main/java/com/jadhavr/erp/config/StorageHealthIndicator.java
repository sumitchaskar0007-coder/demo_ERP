package com.jadhavr.erp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component("storage")
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class StorageHealthIndicator implements HealthIndicator {
    private final Path uploadDirectory;
    private final Path admissionPhotoDirectory;

    public StorageHealthIndicator(
            @Value("${app.upload-dir:uploads}") String uploadDirectory,
            @Value("${app.storage.admission-photo-dir:uploads/admission-photos}") String admissionPhotoDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.admissionPhotoDirectory = Path.of(admissionPhotoDirectory).toAbsolutePath().normalize();
    }

    @Override
    public Health health() {
        try {
            Files.createDirectories(uploadDirectory);
            Files.createDirectories(admissionPhotoDirectory);
            if (!Files.isWritable(uploadDirectory) || !Files.isWritable(admissionPhotoDirectory)) {
                return Health.down().withDetail("reason", "Shared storage is not writable").build();
            }
            return Health.up().build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
