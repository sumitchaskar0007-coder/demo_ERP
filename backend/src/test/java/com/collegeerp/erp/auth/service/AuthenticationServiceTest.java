package com.collegeerp.erp.auth.service;

import com.collegeerp.erp.auth.repository.RefreshTokenRepository;
import com.collegeerp.erp.auth.entity.RefreshToken;
import com.collegeerp.erp.auth.dto.LoginRequest;
import com.collegeerp.erp.auth.security.JwtService;
import com.collegeerp.erp.auth.util.TokenHashUtil;
import com.collegeerp.erp.common.exception.TooManyRequestsException;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.mapper.UserMapper;
import com.collegeerp.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private UserRepository users;
    @Mock private UserMapper mapper;
    @Mock private TokenHashUtil hashes;
    @Mock private SecurityEventService securityEvents;
    @Mock private DistributedRateLimiter rateLimiter;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(authenticationManager, jwtService, refreshTokens, users, mapper,
                hashes, securityEvents, rateLimiter, 20, 6);
    }

    @Test
    void logoutRevokesOnlyTheCurrentDeviceRefreshToken() {
        User user = new User();
        user.setId(7L);
        user.setFullName("User");
        when(hashes.hash("current-device-token")).thenReturn("current-device-hash");
        when(users.findById(7L)).thenReturn(Optional.of(user));

        service.logout(7L, "current-device-token", "127.0.0.1", "test-agent");

        verify(refreshTokens).revokeCurrentSession(7L, "current-device-hash");
        verify(refreshTokens, never()).revokeAllForUser(7L);
        verify(users, never()).save(user);
        verify(securityEvents).audit(7L, null, "LOGOUT", true, "127.0.0.1", "test-agent",
                "Current device session revoked");
    }

    @Test
    void loginUsesNormalizedAccountGlobalAndAccountIpBuckets() {
        when(rateLimiter.check(
                eq("auth:login-account"),
                eq("student@example.com"),
                eq(20L),
                any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(false, 23));
        when(rateLimiter.check(
                eq("auth:login-account-ip"),
                eq("student@example.com|203.0.113.8"),
                eq(6L),
                any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));

        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> service.login(
                        new LoginRequest(" Student@Example.com ", "not-logged"),
                        "203.0.113.8",
                        "test-agent"));

        assertEquals(23, exception.getRetryAfterSeconds());
        verify(authenticationManager, never()).authenticate(any());
        verify(rateLimiter).check(
                eq("auth:login-account-ip"),
                eq("student@example.com|203.0.113.8"),
                eq(6L),
                any(Duration.class));
    }

    @Test
    void concurrentRefreshLoserIsTreatedAsTokenReuse() {
        User user = new User();
        user.setId(7L);
        user.setFullName("User");
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash("refresh-hash");
        stored.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(hashes.hash("refresh-token")).thenReturn("refresh-hash");
        when(refreshTokens.findByTokenHash("refresh-hash")).thenReturn(Optional.of(stored));
        when(refreshTokens.consume("refresh-hash")).thenReturn(0);

        assertThrows(
                org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> service.rotate("refresh-token", "127.0.0.1", "test-agent"));

        verify(securityEvents).refreshReuse(7L, "127.0.0.1", "test-agent");
        verify(jwtService, never()).generateAccessToken(any());
    }
}
