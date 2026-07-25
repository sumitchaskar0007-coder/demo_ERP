package com.jadhavr.erp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;

/** Rejects unsafe URL/CORS defaults when running with the production profile. */
@Component
@Profile("production")
public class ProductionWebConfigValidator {
    private final String frontendUrl;
    private final String corsOrigins;
    public ProductionWebConfigValidator(@Value("${app.frontend-url}") String frontendUrl,
                                        @Value("${app.cors.allowed-origins}") String corsOrigins) {
        this.frontendUrl = frontendUrl;
        this.corsOrigins = corsOrigins;
    }

    @PostConstruct
    void validate() {
        requireOrigin(frontendUrl, "FRONTEND_URL");
        if (corsOrigins == null || corsOrigins.isBlank()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS is required");
        }
        if (corsOrigins.contains("*")) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must not contain wildcard origins");
        }
        Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(value -> requireOrigin(value, "CORS_ALLOWED_ORIGINS"));
    }

    private static void requireOrigin(String value, String name) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException(name + " is required");
        }
        URI uri;
        try { uri = URI.create(value); } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(name + " must contain valid absolute HTTPS origins");
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1") ||
                uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(name + " must contain HTTPS public origins without credentials or query strings");
        }
    }
}
