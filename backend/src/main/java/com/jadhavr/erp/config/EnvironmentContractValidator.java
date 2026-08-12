package com.jadhavr.erp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Ensures every runtime selects exactly one supported deployment environment. */
@Component
public class EnvironmentContractValidator {
    private static final Set<String> SUPPORTED = Set.of("local", "preprod", "production");
    private static final Set<String> RETIRED = Set.of("docker", "staging");

    private final Environment environment;
    private final String configuredEnvironment;

    public EnvironmentContractValidator(
            Environment environment,
            @Value("${app.environment:}") String configuredEnvironment) {
        this.environment = environment;
        this.configuredEnvironment = configuredEnvironment;
    }

    @PostConstruct
    void validate() {
        Set<String> active = new LinkedHashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (active.isEmpty()) {
            active.addAll(Arrays.asList(environment.getDefaultProfiles()));
        }

        Set<String> retired = new LinkedHashSet<>(active);
        retired.retainAll(RETIRED);
        if (!retired.isEmpty()) {
            throw new IllegalStateException(
                    "Retired Spring profiles are not allowed: " + retired
                            + ". Use exactly one of local, preprod, or production.");
        }

        Set<String> selected = new LinkedHashSet<>(active);
        selected.retainAll(SUPPORTED);
        if (selected.size() != 1) {
            throw new IllegalStateException(
                    "Exactly one deployment profile is required: local, preprod, or production. Active profiles: "
                            + active);
        }

        String profile = selected.iterator().next();
        if (!profile.equals(configuredEnvironment)) {
            throw new IllegalStateException(
                    "Deployment profile " + profile + " does not match app.environment="
                            + configuredEnvironment);
        }
    }
}
