package com.jadhavr.erp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionWebConfigValidatorTest {
    @Test
    void acceptsHttpsExactOrigins() {
        var validator = new ProductionWebConfigValidator(
                "https://erp.example.com", "https://erp.example.com,https://admin.example.com");
        assertDoesNotThrow(validator::validate);
    }

    @Test
    void rejectsWildcardAndDevelopmentOrigins() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionWebConfigValidator("http://localhost:5173", "*").validate());
    }
}
