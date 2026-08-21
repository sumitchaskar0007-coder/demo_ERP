package com.collegeerp.erp.email.service;

import com.collegeerp.erp.auth.repository.RefreshTokenRepository;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import com.collegeerp.erp.auth.service.DistributedRateLimiter;
import com.collegeerp.erp.email.config.MailProperties;
import com.collegeerp.erp.email.entity.EmailVerificationToken;
import com.collegeerp.erp.email.entity.PasswordResetToken;
import com.collegeerp.erp.email.repository.EmailVerificationTokenRepository;
import com.collegeerp.erp.email.repository.PasswordResetTokenRepository;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class AccountTokenService {
    private final UserRepository users;
    private final PasswordResetTokenRepository resets;
    private final EmailVerificationTokenRepository verifications;
    private final EmailNotificationService emails;
    private final PasswordEncoder encoder;
    private final MailProperties properties;
    private final DistributedRateLimiter rateLimiter;
    private final RefreshTokenRepository refreshTokens;
    private final AuthorizationSnapshotService authorizationSnapshots;
    private final SecureRandom random = new SecureRandom();

    public AccountTokenService(
            UserRepository users,
            PasswordResetTokenRepository resets,
            EmailVerificationTokenRepository verifications,
            EmailNotificationService emails,
            PasswordEncoder encoder,
            MailProperties properties,
            DistributedRateLimiter rateLimiter,
            RefreshTokenRepository refreshTokens,
            AuthorizationSnapshotService authorizationSnapshots) {
        this.users = users;
        this.resets = resets;
        this.verifications = verifications;
        this.emails = emails;
        this.encoder = encoder;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.refreshTokens = refreshTokens;
        this.authorizationSnapshots = authorizationSnapshots;
    }

    @Transactional
    public void forgot(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (limited("reset:" + normalized)) {
            return;
        }
        users.findByEmail(normalized).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            resets.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(user.getId())
                    .forEach(token -> token.setRevokedAt(now));
            String raw = token();
            PasswordResetToken reset = new PasswordResetToken();
            reset.setUser(user);
            reset.setTokenHash(hash(raw));
            reset.setExpiresAt(now.plusMinutes(properties.getTokenExpiryMinutes()));
            resets.save(reset);
            emails.queuePasswordResetEmail(user, raw, reset.getExpiresAt());
        });
    }

    @Transactional
    public void reset(String raw, String password) {
        PasswordResetToken reset = resets.findByTokenHash(hash(raw))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired password reset token"));
        validate(reset.getUsedAt(), reset.getRevokedAt(), reset.getExpiresAt());
        User user = reset.getUser();
        user.setPasswordHash(encoder.encode(password));
        user.setMustChangePassword(false);
        user.setSessionVersion(user.getSessionVersion() + 1);
        users.save(user);
        refreshTokens.revokeAllForUser(user.getId());
        authorizationSnapshots.invalidateOrThrow(user.getId());
        reset.setUsedAt(LocalDateTime.now());
        resets.save(reset);
        emails.queuePasswordChangedEmail(user);
    }

    @Transactional
    public void requestVerification(User user) {
        if (limited("verify:" + user.getEmail())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        verifications.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(user.getId())
                .forEach(token -> token.setRevokedAt(now));
        String raw = token();
        EmailVerificationToken verification = new EmailVerificationToken();
        verification.setUser(user);
        verification.setTokenHash(hash(raw));
        verification.setExpiresAt(now.plusMinutes(properties.getTokenExpiryMinutes()));
        verifications.save(verification);
        emails.queueEmailVerificationEmail(user, raw, verification.getExpiresAt());
    }

    @Transactional
    public void verify(String raw) {
        EmailVerificationToken verification = verifications.findByTokenHash(hash(raw))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired verification token"));
        validate(verification.getUsedAt(), verification.getRevokedAt(),
                verification.getExpiresAt());
        User user = verification.getUser();
        user.setEmailVerified(true);
        users.save(user);
        verification.setUsedAt(LocalDateTime.now());
        verifications.save(verification);
        emails.queueEmailVerifiedEmail(user);
    }

    private boolean limited(String key) {
        return !rateLimiter.tryAcquire(
                "account-token", key, 1, Duration.ofMinutes(1));
    }

    private void validate(
            LocalDateTime used,
            LocalDateTime revoked,
            LocalDateTime expiry) {
        if (used != null || revoked != null || expiry.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
