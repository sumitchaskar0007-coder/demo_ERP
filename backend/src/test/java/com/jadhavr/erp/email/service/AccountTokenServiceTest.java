package com.jadhavr.erp.email.service;

import com.jadhavr.erp.email.config.MailProperties;
import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import com.jadhavr.erp.email.entity.EmailVerificationToken;
import com.jadhavr.erp.email.entity.PasswordResetToken;
import com.jadhavr.erp.email.repository.EmailVerificationTokenRepository;
import com.jadhavr.erp.email.repository.PasswordResetTokenRepository;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountTokenServiceTest {

    @Mock private UserRepository users;
    @Mock private PasswordResetTokenRepository resets;
    @Mock private EmailVerificationTokenRepository verifications;
    @Mock private EmailNotificationService emails;
    @Mock private PasswordEncoder encoder;
    @Mock private DistributedRateLimiter rateLimiter;

    private AccountTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setTokenExpiryMinutes(30);
        service = new AccountTokenService(users, resets, verifications, emails, encoder, properties, rateLimiter);

        user = new User();
        user.setId(42L);
        user.setEmail("student@example.com");
        user.setFullName("Test Student");
        user.setPasswordHash("old-hash");
    }

    @Test
    void forgotDoesNotRevealUnknownEmailAndCreatesNothing() {
        when(rateLimiter.tryAcquire(any(), any(), any(Long.class), any())).thenReturn(true);
        when(users.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.forgot("unknown@example.com");

        verify(resets, never()).save(any());
        verify(emails, never()).queuePasswordResetEmail(any(), any(), any());
    }

    @Test
    void forgotIsRateLimitedPerNormalizedAccount() {
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(resets.findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(42L)).thenReturn(List.of());

        when(rateLimiter.tryAcquire(any(), any(), any(Long.class), any())).thenReturn(true, false);
        service.forgot(" Student@Example.com ");
        service.forgot("student@example.com");

        verify(resets, times(1)).save(any());
        verify(emails, times(1)).queuePasswordResetEmail(any(), any(), any());
    }

    @Test
    void validResetTokenIsSingleUse() {
        String rawToken = "valid-reset-token-that-is-long-enough-for-a-test";
        PasswordResetToken token = resetToken(rawToken, LocalDateTime.now().plusMinutes(5));
        when(resets.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));
        when(encoder.encode("Secure@123")).thenReturn("new-hash");

        service.reset(rawToken, "Secure@123");

        assertTrue(token.getUsedAt() != null);
        verify(users).save(user);
        verify(emails).queuePasswordChangedEmail(user);
        assertThrows(IllegalArgumentException.class,
                () -> service.reset(rawToken, "Another@123"));
    }

    @Test
    void expiredAndInvalidResetTokensAreRejectedSafely() {
        String expiredRaw = "expired-reset-token-that-is-long-enough-for-test";
        when(resets.findByTokenHash(sha256(expiredRaw)))
                .thenReturn(Optional.of(resetToken(expiredRaw, LocalDateTime.now().minusSeconds(1))));

        assertThrows(IllegalArgumentException.class,
                () -> service.reset(expiredRaw, "Secure@123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.reset("missing-token", "Secure@123"));
        verify(users, never()).save(any());
    }

    @Test
    void validVerificationTokenIsConsumedAndExpiredTokenIsRejected() {
        String validRaw = "valid-verification-token-that-is-long-enough";
        EmailVerificationToken valid = verificationToken(validRaw, LocalDateTime.now().plusMinutes(5));
        when(verifications.findByTokenHash(sha256(validRaw))).thenReturn(Optional.of(valid));

        service.verify(validRaw);

        assertTrue(user.isEmailVerified());
        assertTrue(valid.getUsedAt() != null);
        verify(emails).queueEmailVerifiedEmail(user);

        String expiredRaw = "expired-verification-token-that-is-long-enough";
        when(verifications.findByTokenHash(sha256(expiredRaw)))
                .thenReturn(Optional.of(verificationToken(expiredRaw, LocalDateTime.now().minusSeconds(1))));
        assertThrows(IllegalArgumentException.class, () -> service.verify(expiredRaw));
    }

    private PasswordResetToken resetToken(String raw, LocalDateTime expiry) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(expiry);
        return token;
    }

    private EmailVerificationToken verificationToken(String raw, LocalDateTime expiry) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(expiry);
        return token;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
