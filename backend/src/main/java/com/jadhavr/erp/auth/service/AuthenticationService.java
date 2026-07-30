package com.jadhavr.erp.auth.service;

import com.jadhavr.erp.auth.dto.AuthUserResponse;
import com.jadhavr.erp.auth.dto.LoginRequest;
import com.jadhavr.erp.auth.entity.RefreshToken;
import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.auth.util.TokenHashUtil;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.mapper.UserMapper;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.core.AuthenticationException;

@Service
public class AuthenticationService {
    public record TokenPair(String accessToken, String refreshToken, AuthUserResponse user) {}
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final UserMapper mapper;
    private final TokenHashUtil hashes;
    private final SecurityEventService securityEvents;
    private final DistributedRateLimiter rateLimiter;
    private final long loginAccountLimit;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService,
            RefreshTokenRepository refreshTokens, UserRepository users, UserMapper mapper, TokenHashUtil hashes,
            SecurityEventService securityEvents, DistributedRateLimiter rateLimiter,
            @Value("${app.rate-limit.login-account-per-minute:5}") long loginAccountLimit) {
        this.authenticationManager = authenticationManager; this.jwtService = jwtService;
        this.refreshTokens = refreshTokens; this.users = users; this.mapper = mapper; this.hashes = hashes;
        this.securityEvents = securityEvents;
        this.rateLimiter = rateLimiter;
        this.loginAccountLimit = loginAccountLimit;
    }

    @Transactional
    public TokenPair login(LoginRequest request, String ip, String userAgent) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.tryAcquire("auth:login-account-ip", email + "|" + ip,
                loginAccountLimit, Duration.ofMinutes(1))) {
            throw new com.jadhavr.erp.common.exception.TooManyRequestsException(
                    "Too many login attempts. Please retry after 60 seconds");
        }
        org.springframework.security.core.Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            securityEvents.loginFailure(email, ip, userAgent);
            throw exception;
        }
        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        User user = users.findByEmail(email).orElseThrow();
        user.setLastLoginAt(LocalDateTime.now()); user.setFailedLoginAttempts(0); user.setLockedUntil(null); users.save(user);
        securityEvents.audit(user.getId(), details.getCollegeId(), "LOGIN_SUCCESS", true, ip, userAgent, "Cookie session issued");
        return issue(user, details);
    }

    @Transactional
    public TokenPair rotate(String rawRefreshToken, String ip, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) throw unauthorized();
        RefreshToken stored = refreshTokens.findByTokenHash(hashes.hash(rawRefreshToken)).orElseThrow(this::unauthorized);
        if (stored.isRevoked()) {
            securityEvents.refreshReuse(stored.getUser().getId(), ip, userAgent);
            throw unauthorized();
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) throw unauthorized();
        User user = stored.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) throw unauthorized();
        Long storedInstitution = stored.getInstitution() == null ? null : stored.getInstitution().getId();
        Long userInstitution = user.getCollege() == null ? null : user.getCollege().getId();
        if (!java.util.Objects.equals(storedInstitution, userInstitution)) throw unauthorized();
        stored.setRevoked(true); refreshTokens.save(stored);
        securityEvents.audit(user.getId(), userInstitution, "TOKEN_REFRESH", true, ip, userAgent, "Refresh token rotated");
        return issue(user, new CustomUserDetails(user));
    }

    @Transactional
    public void logout(Long userId, String rawRefreshToken, String ip, String userAgent) {
        if (userId == null) return;
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revokeCurrentSession(userId, hashes.hash(rawRefreshToken));
        }
        User user = users.findById(userId).orElse(null);
        if (user != null) {
            securityEvents.audit(userId, user.getCollege() == null ? null : user.getCollege().getId(),
                    "LOGOUT", true, ip, userAgent, "Current device session revoked");
        }
    }

    private TokenPair issue(User user, CustomUserDetails details) {
        String rawRefresh = hashes.newRefreshToken();
        LocalDateTime now = LocalDateTime.now();
        RefreshToken stored = new RefreshToken(); stored.setUser(user); stored.setInstitution(user.getCollege());
        stored.setTokenHash(hashes.hash(rawRefresh)); stored.setIssuedAt(now); stored.setExpiresAt(now.plusDays(7));
        refreshTokens.save(stored);
        return new TokenPair(jwtService.generateAccessToken(details), rawRefresh, mapper.toAuthResponse(user));
    }
    private AuthenticationCredentialsNotFoundException unauthorized() { return new AuthenticationCredentialsNotFoundException("Invalid refresh token"); }
}
