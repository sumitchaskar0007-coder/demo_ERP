package com.jadhavr.erp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Prevents managed AWS environments from starting with absent or development credentials. */
@Component
@Profile("preprod | production")
public class ProductionSecretsValidator {
    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String jwtSecret;
    private final boolean bootstrapEnabled;
    private final String superAdminPassword;
    private final String redisHost;
    private final String redisPassword;
    private final boolean mailEnabled;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final String awsRegion;
    private final String uploadBucket;
    private final String secretsName;

    public ProductionSecretsValidator(
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.bootstrap.enabled:false}") boolean bootstrapEnabled,
            @Value("${app.super-admin.password}") String superAdminPassword,
            @Value("${spring.data.redis.host}") String redisHost,
            @Value("${spring.data.redis.password}") String redisPassword,
            @Value("${app.mail.enabled:true}") boolean mailEnabled,
            @Value("${spring.mail.host}") String mailHost,
            @Value("${spring.mail.username}") String mailUsername,
            @Value("${spring.mail.password}") String mailPassword,
            @Value("${app.aws.region}") String awsRegion,
            @Value("${app.aws.private-upload-bucket}") String uploadBucket,
            @Value("${app.aws.secrets-name}") String secretsName) {
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.jwtSecret = jwtSecret;
        this.bootstrapEnabled = bootstrapEnabled;
        this.superAdminPassword = superAdminPassword;
        this.redisHost = redisHost;
        this.redisPassword = redisPassword;
        this.mailEnabled = mailEnabled;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.awsRegion = awsRegion;
        this.uploadBucket = uploadBucket;
        this.secretsName = secretsName;
    }

    @PostConstruct
    void validate() {
        List<String> invalid = new ArrayList<>();
        required(invalid, "DB_URL", databaseUrl);
        required(invalid, "DB_USERNAME", databaseUsername);
        required(invalid, "DB_PASSWORD", databasePassword);
        required(invalid, "JWT_SECRET", jwtSecret);
        if (bootstrapEnabled) {
            required(invalid, "SUPER_ADMIN_PASSWORD", superAdminPassword);
        }
        required(invalid, "REDIS_HOST", redisHost);
        required(invalid, "REDIS_PASSWORD", redisPassword);
        if (mailEnabled) {
            required(invalid, "MAIL_HOST", mailHost);
            required(invalid, "MAIL_USERNAME", mailUsername);
            required(invalid, "MAIL_PASSWORD", mailPassword);
        }
        required(invalid, "AWS_REGION", awsRegion);
        required(invalid, "AWS_PRIVATE_UPLOAD_BUCKET", uploadBucket);
        required(invalid, "AWS_SECRETS_NAME", secretsName);

        rejectLocalHost(invalid, "DB_URL", databaseUrl);
        rejectLocalHost(invalid, "REDIS_HOST", redisHost);
        if (mailEnabled) {
            rejectLocalHost(invalid, "MAIL_HOST", mailHost);
        }
        if (jwtSecret == null || jwtSecret.length() < 32 || contains(jwtSecret, "change_this")) {
            invalid.add("JWT_SECRET must be a random value of at least 32 characters");
        }
        if (bootstrapEnabled && (superAdminPassword == null || superAdminPassword.length() < 12)) {
            invalid.add("SUPER_ADMIN_PASSWORD must be non-default and at least 12 characters");
        }
        if ("postgres".equals(databasePassword)) {
            invalid.add("DB_PASSWORD must not use the development default");
        }

        if (!invalid.isEmpty()) {
            throw new IllegalStateException("Unsafe managed-environment configuration: " + String.join("; ", invalid));
        }
    }

    private static void required(List<String> invalid, String name, String value) {
        if (value == null || value.isBlank() || value.startsWith("${")) {
            invalid.add(name + " is required");
        }
    }

    private static void rejectLocalHost(List<String> invalid, String name, String value) {
        if (value == null) return;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
            invalid.add(name + " must not point to localhost");
        }
    }

    private static boolean contains(String value, String token) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(token);
    }
}
