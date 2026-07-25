package com.jadhavr.erp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionDatabaseTlsValidatorTest {
    @Test
    void requiresVerifyFullAndCaBundle() {
        assertDoesNotThrow(() -> new ProductionDatabaseTlsValidator(
                "jdbc:postgresql://rds.example/erp?sslmode=verify-full", "/etc/ssl/rds-ca.pem").validate());
        assertThrows(IllegalStateException.class, () -> new ProductionDatabaseTlsValidator(
                "jdbc:postgresql://rds.example/erp", "/etc/ssl/rds-ca.pem").validate());
        assertThrows(IllegalStateException.class, () -> new ProductionDatabaseTlsValidator(
                "jdbc:postgresql://rds.example/erp?sslmode=verify-full", "").validate());
    }
}
