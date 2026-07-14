package com.jadhavr.erp.common.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        LocalDateTime timestamp,
        Map<String, String> errors,
        String path
) {
    public ErrorResponse(String message) {
        this(false, message, LocalDateTime.now(), null, null);
    }
    public ErrorResponse(String message, String path) { this(false, message, LocalDateTime.now(), null, path); }
    public ErrorResponse(boolean success, String message, LocalDateTime timestamp, Map<String, String> errors) {
        this(success, message, timestamp, errors, null);
    }
}
