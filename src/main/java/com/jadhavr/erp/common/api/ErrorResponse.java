package com.jadhavr.erp.common.api;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        LocalDateTime timestamp,
        Map<String, String> errors
) {
    public ErrorResponse(String message) {
        this(false, message, LocalDateTime.now(), null);
    }
}
