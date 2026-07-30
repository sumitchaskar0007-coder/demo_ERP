package com.jadhavr.erp.auth.service;

import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.auth.util.TokenHashUtil;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;

import static org.mockito.Mockito.*;

class AuthenticationServiceTest {
    @Test
    void logoutRevokesOnlyTheRefreshTokenForTheCurrentDevice() {
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        SecurityEventService events = mock(SecurityEventService.class);
        TokenHashUtil hashes = new TokenHashUtil();
        User user = new User();
        user.setId(42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        AuthenticationService service = new AuthenticationService(
                mock(AuthenticationManager.class), mock(JwtService.class), refreshTokens,
                users, mock(UserMapper.class), hashes, events,
                mock(DistributedRateLimiter.class), 5);

        service.logout(42L, "device-refresh-token", "203.0.113.5", "browser");

        verify(refreshTokens).revokeCurrentSession(42L, hashes.hash("device-refresh-token"));
        verify(refreshTokens, never()).revokeAllForUser(anyLong());
        verify(users, never()).save(any());
        verify(events).audit(42L, null, "LOGOUT", true,
                "203.0.113.5", "browser", "Current device session revoked");
    }
}
