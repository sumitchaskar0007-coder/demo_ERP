package com.jadhavr.erp.storage;

import com.jadhavr.erp.common.exception.BadRequestException;

import java.util.regex.Pattern;

final class ObjectKeyPolicy {
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9/_.=-]{0,1023}");

    private ObjectKeyPolicy() {}

    static String requireSafe(String key) {
        if (key == null || key.startsWith("/") || key.contains("..") || !SAFE_KEY.matcher(key).matches()) {
            throw new BadRequestException("Invalid storage object key");
        }
        return key;
    }
}
