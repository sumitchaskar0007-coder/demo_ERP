package com.jadhavr.erp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component("storage")
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
            return Health.up()
                    .withDetail("uploadDirectory", uploadDirectory.toString())
                    .withDetail("admissionPhotoDirectory", admissionPhotoDirectory.toString())
                    .build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
