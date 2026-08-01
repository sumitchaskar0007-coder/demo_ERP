package com.jadhavr.erp.auth.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryPasswordGeneratorTest {
    @Test
    void generatesUniqueComplexPasswordsWithoutAmbiguousCharacters() {
        Set<String> generated = new HashSet<>();
        for (int index = 0; index < 100; index++) {
            String password = TemporaryPasswordGenerator.generate();
            assertEquals(20, password.length());
            assertTrue(password.matches(".*[a-z].*"));
            assertTrue(password.matches(".*[A-Z].*"));
            assertTrue(password.matches(".*[0-9].*"));
            assertTrue(password.matches(".*[!@#$%*\\-_=+].*"));
            assertTrue(password.chars().noneMatch(value -> "0O1Il".indexOf(value) >= 0));
            generated.add(password);
        }
        assertEquals(100, generated.size());
    }
}
