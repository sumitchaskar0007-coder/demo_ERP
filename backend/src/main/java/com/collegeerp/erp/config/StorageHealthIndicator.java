package com.collegeerp.erp.config;

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
    private final Path objectDirectory;

    public StorageHealthIndicator(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.objectDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("objects");
    }

    @Override
    public Health health() {
        try {
            Files.createDirectories(objectDirectory);
            if (!Files.isWritable(objectDirectory)) {
                return Health.down().withDetail("reason", "Object storage is not writable").build();
            }
            return Health.up().build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
