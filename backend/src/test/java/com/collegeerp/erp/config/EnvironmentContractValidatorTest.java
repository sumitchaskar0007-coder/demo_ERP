package com.collegeerp.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentContractValidatorTest {
    @Test
    void acceptsEachSupportedEnvironmentByItself() {
        for (String profile : new String[]{"local", "preprod", "production"}) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles(profile);
            assertDoesNotThrow(() -> new EnvironmentContractValidator(environment, profile).validate());
        }
    }

    @Test
    void rejectsMixedAndRetiredProfiles() {
        MockEnvironment mixed = new MockEnvironment();
        mixed.setActiveProfiles("preprod", "production");
        assertThrows(IllegalStateException.class,
                () -> new EnvironmentContractValidator(mixed, "preprod").validate());

        MockEnvironment retired = new MockEnvironment();
        retired.setActiveProfiles("staging", "production");
        assertThrows(IllegalStateException.class,
                () -> new EnvironmentContractValidator(retired, "production").validate());
    }

    @Test
    void rejectsProfileAndPropertyMismatch() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("preprod");
        assertThrows(IllegalStateException.class,
                () -> new EnvironmentContractValidator(environment, "production").validate());
    }
}
