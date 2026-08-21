package com.collegeerp.erp.bootstrap;

import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.entity.UserStatus;
import com.collegeerp.erp.user.repository.RoleRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.auth.repository.RefreshTokenRepository;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPhone;
    private final String adminPassword;
    private final boolean resetExistingPassword;
    private final RefreshTokenRepository refreshTokens;
    private final AuthorizationSnapshotService authorizationSnapshots;

    public DataSeeder(RoleRepository roles, UserRepository users, PasswordEncoder encoder,
            RefreshTokenRepository refreshTokens,
            AuthorizationSnapshotService authorizationSnapshots,
            @Value("${app.super-admin.name}") String adminName,
            @Value("${app.super-admin.email}") String adminEmail,
            @Value("${app.super-admin.phone}") String adminPhone,
            @Value("${app.super-admin.password}") String adminPassword,
            @Value("${app.bootstrap.reset-existing-password:false}") boolean resetExistingPassword) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.authorizationSnapshots = authorizationSnapshots;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
        this.resetExistingPassword = resetExistingPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();

        if (adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "A bootstrap administrator password of at least 12 characters is required");
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank() || adminName == null || adminName.isBlank()) {
            throw new IllegalStateException("Bootstrap administrator name and email are required");
        }
        if (users.existsByEmail(email)) {
            if (resetExistingPassword) {
                User admin = users.findByEmail(email).orElseThrow();
                admin.setPasswordHash(encoder.encode(adminPassword));
                admin.setMustChangePassword(false);
                admin.setSessionVersion(admin.getSessionVersion() + 1);
                users.save(admin);
                refreshTokens.revokeAllForUser(admin.getId());
                authorizationSnapshots.invalidateOrThrow(admin.getId());
                log.info("Existing administrator password synchronized for local bootstrap");
                return;
            }
            log.info("Administrator bootstrap skipped because the account already exists");
            return;
        }

        Role role = roles.findByName(RoleName.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role bootstrap failed"));
        User admin = new User();
        admin.setFullName(adminName.trim());
        admin.setEmail(email);
        admin.setPhone(adminPhone == null || adminPhone.isBlank() ? null : adminPhone.trim());
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(Set.of(role));
        users.save(admin);
        log.info("Administrator bootstrap completed");
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roles.findByName(roleName).isPresent()) {
                continue;
            }
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(roleName.name().replace('_', ' ') + " role");
            roles.save(role);
        }
        log.info("Role bootstrap completed");
    }
}
