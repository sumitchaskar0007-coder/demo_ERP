package com.collegeerp.erp.auth.util;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {
    private static final int LENGTH = 20;
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%*-_=+".toCharArray();
    private static final char[] ALL = (
            new String(LOWER) + new String(UPPER) + new String(DIGITS) + new String(SYMBOLS))
            .toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        char[] password = new char[LENGTH];
        password[0] = random(LOWER);
        password[1] = random(UPPER);
        password[2] = random(DIGITS);
        password[3] = random(SYMBOLS);
        for (int index = 4; index < password.length; index++) {
            password[index] = random(ALL);
        }
        for (int index = password.length - 1; index > 0; index--) {
            int swap = RANDOM.nextInt(index + 1);
            char value = password[index];
            password[index] = password[swap];
            password[swap] = value;
        }
        return new String(password);
    }

    private static char random(char[] values) {
        return values[RANDOM.nextInt(values.length)];
    }
}
