package com.jadhavr.erp.auth.password;

import com.jadhavr.erp.auth.util.TemporaryPasswordGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class SecureRandomInitialPasswordPolicy implements InitialPasswordPolicy {
    @Override
    public String create(String phone) {
        return TemporaryPasswordGenerator.generate();
    }
}
