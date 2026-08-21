package com.collegeerp.erp.auth.password;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialPasswordPolicyTest {
    @Test
    void localPolicyUsesTrimmedPhoneNumber() {
        var policy = new LocalPhoneInitialPasswordPolicy();

        assertEquals("9876543210", policy.create(" 9876543210 "));
        assertThrows(IllegalArgumentException.class, () -> policy.create(" "));
    }

    @Test
    void nonLocalPolicyKeepsSecureRandomPasswordBehavior() {
        var policy = new SecureRandomInitialPasswordPolicy();

        String password = policy.create("9876543210");

        assertNotEquals("9876543210", password);
        assertTrue(password.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%*\\-_=+]).{20}$"));
    }

    @Test
    void springProfilesKeepPoliciesMutuallyExclusive() {
        assertArrayEquals(
                new String[]{"local"},
                LocalPhoneInitialPasswordPolicy.class.getAnnotation(Profile.class).value());
        assertArrayEquals(
                new String[]{"!local"},
                SecureRandomInitialPasswordPolicy.class.getAnnotation(Profile.class).value());
    }
}
