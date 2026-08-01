package com.jadhavr.erp.common.error;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestCorrelation {
    public static final String HEADER = "X-Request-ID";
    public static final String ATTRIBUTE = RequestCorrelation.class.getName() + ".id";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

    private RequestCorrelation() {
    }

    public static String getOrCreate(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTRIBUTE);
        if (existing instanceof String value && SAFE_ID.matcher(value).matches()) {
            return value;
        }
        String supplied = request.getHeader(HEADER);
        String correlationId = supplied != null && SAFE_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, correlationId);
        return correlationId;
    }
}
