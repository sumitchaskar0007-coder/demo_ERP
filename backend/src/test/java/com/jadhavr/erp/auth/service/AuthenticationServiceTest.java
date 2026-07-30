package com.jadhavr.erp.auth.service;

import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.auth.dto.LoginRequest;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.auth.util.TokenHashUtil;
import com.jadhavr.erp.common.exception.TooManyRequestsException;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;
import java.time.Duration;

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
                hashes, securityEvents, rateLimiter, 20, 5);
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
                eq(5L),
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
                eq(5L),
                any(Duration.class));
    }
}
