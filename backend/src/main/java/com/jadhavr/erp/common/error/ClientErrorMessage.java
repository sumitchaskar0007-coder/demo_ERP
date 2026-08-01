package com.jadhavr.erp.common.error;

import java.util.regex.Pattern;

/** Prevents internal diagnostic details from being rendered in client responses. */
public final class ClientErrorMessage {
    private static final int MAX_LENGTH = 300;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile(
            "[\\r\\n\\t\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    private static final Pattern SENSITIVE_DETAILS = Pattern.compile(
            "(?i)(?:"
                    + "[a-z]:\\\\[^\\s]+"
                    + "|/(?:home|users?|var|opt|etc|srv|tmp|app|workspace)/[^\\s]+"
                    + "|\\b(?:at\\s+)?[\\w.$]+\\([^\\r\\n()]+\\.(?:java|kt|js|ts|tsx|jsx):\\d+"
                    + "|\\b(?:sqlstate|sql|jdbc|hibernate|postgres(?:ql)?|mysql|oracle|mariadb|sqlite)\\b"
                    + "|\\b(?:constraint|relation|table|column)\\s+[\"'`]?[a-z0-9_.-]+"
                    + "|\\b(?:caused by|exception in thread|stack trace)\\b"
                    + ")");

    private ClientErrorMessage() {
    }

    public static String safe(String candidate, String fallback) {
        if (candidate == null) {
            return fallback;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()
                || trimmed.length() > MAX_LENGTH
                || CONTROL_CHARACTERS.matcher(trimmed).find()
                || SENSITIVE_DETAILS.matcher(trimmed).find()) {
            return fallback;
        }
        return trimmed;
    }
}
