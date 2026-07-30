package com.jadhavr.erp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageHealthIndicatorTest {

    @TempDir
    Path uploadDirectory;

    @Test
    void createsAndChecksTheActualObjectDirectory() {
        StorageHealthIndicator indicator = new StorageHealthIndicator(uploadDirectory.toString());

        assertEquals(Status.UP, indicator.health().getStatus());
        assertTrue(Files.isDirectory(uploadDirectory.resolve("objects")));
    }
}
