package com.jadhavr.erp.auth.password;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPhoneInitialPasswordPolicy implements InitialPasswordPolicy {
    @Override
    public String create(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required for the local initial password");
        }
        return phone.trim();
    }
}
