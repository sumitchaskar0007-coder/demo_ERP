package com.jadhavr.erp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Ensures production PostgreSQL uses hostname and CA verification. */
@Component
@Profile("production")
public class ProductionDatabaseTlsValidator {
    private final String url;
    private final String rootCert;
    public ProductionDatabaseTlsValidator(@Value("${spring.datasource.url}") String url,
                                          @Value("${spring.datasource.hikari.data-source-properties.sslrootcert}") String rootCert) {
        this.url = url;
        this.rootCert = rootCert;
    }
    @PostConstruct
    void validate() {
        if (url == null || !url.toLowerCase().contains("sslmode=verify-full")) {
            throw new IllegalStateException("Production DB_URL must use sslmode=verify-full");
        }
        if (rootCert == null || rootCert.isBlank() || rootCert.startsWith("${")) {
            throw new IllegalStateException("DB_SSL_ROOT_CERT must point to the current RDS CA bundle");
        }
    }
}
