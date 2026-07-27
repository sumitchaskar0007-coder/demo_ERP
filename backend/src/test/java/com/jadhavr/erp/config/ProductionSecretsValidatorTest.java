package com.jadhavr.erp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecretsValidatorTest {
    @Test
    void rejectsDevelopmentDefaults() {
        ProductionSecretsValidator validator = new ProductionSecretsValidator(
                "jdbc:postgresql://localhost:5432/college_erp", "postgres", "postgres",
                "CHANGE_THIS_SECRET_TO_A_LONG_RANDOM_SECRET_FOR_PRODUCTION", true, "short-value",
                "localhost", "", true, "localhost", "", "", "ap-south-1", "", "");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void acceptsCompleteExternalProductionConfiguration() {
        ProductionSecretsValidator validator = new ProductionSecretsValidator(
                "jdbc:postgresql://database.internal:5432/college_erp", "erp_app", "db-secret-value",
                "a-random-signing-key-that-is-longer-than-thirty-two-characters", false, "",
                "redis.internal", "redis-secret", true, "smtp.provider.test", "smtp-user", "smtp-secret",
                "ap-south-1", "erp-private-uploads", "production/erp/secrets");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void acceptsDisabledMailWithoutSmtpCredentials() {
        ProductionSecretsValidator validator = new ProductionSecretsValidator(
                "jdbc:postgresql://database.internal:5432/college_erp", "erp_app", "db-secret-value",
                "a-random-signing-key-that-is-longer-than-thirty-two-characters", false, "",
                "redis.internal", "redis-secret", false, "disabled.invalid", "disabled", "disabled",
                "ap-south-1", "erp-private-uploads", "staging/erp/secrets");

        assertDoesNotThrow(validator::validate);
    }
}
