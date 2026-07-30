package com.jadhavr.erp.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jadhavr.erp.auth.security.AuthorizationSnapshot;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.TrustedClientIpResolver;
import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import com.jadhavr.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginRateLimitFilterTest {
    private final DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<DistributedRateLimiter> limiterProvider =
            mock(ObjectProvider.class);
    private final TrustedClientIpResolver clientIps = mock(TrustedClientIpResolver.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TrustedClientIpResolver> clientIpProvider =
            mock(ObjectProvider.class);
    private final LoginRateLimitFilter filter;

    LoginRateLimitFilterTest() {
        when(limiterProvider.getIfAvailable()).thenReturn(limiter);
        when(clientIpProvider.getIfAvailable()).thenReturn(clientIps);
        filter = new LoginRateLimitFilter(
                limiterProvider,
                clientIpProvider,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                1000,
                120,
                600,
                300,
                true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedApiTrafficUsesUserIdNotSharedIp() throws Exception {
        authenticate(42L);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notices/unread-count");
        when(clientIps.resolve(request)).thenReturn("203.0.113.7");
        when(limiter.check(
                eq("http:api-user"), eq("42"), eq(600L), any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                mock(jakarta.servlet.FilterChain.class));

        verify(limiter).check(
                eq("http:api-user"), eq("42"), eq(600L), any(Duration.class));
    }

    @Test
    void anonymousApiTrafficUsesResolvedClientIp() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/public/admissions/options");
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");
        when(limiter.check(
                eq("http:api-anonymous-ip"),
                eq("198.51.100.40"),
                eq(300L),
                any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));

        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                mock(jakarta.servlet.FilterChain.class));

        verify(limiter).check(
                eq("http:api-anonymous-ip"),
                eq("198.51.100.40"),
                eq(300L),
                any(Duration.class));
    }

    @Test
    void rejectedRequestReturnsConsistentJsonAndActualRetryAfter() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");
        when(limiter.check(
                eq("http:login-ip"),
                eq("198.51.100.40"),
                eq(1000L),
                any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(false, 17));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                mock(jakarta.servlet.FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("17");
        assertThat(response.getContentAsString())
                .contains("\"success\":false")
                .contains("\"message\":\"Too many requests\"")
                .contains("\"path\":\"/api/v1/auth/login\"");
    }

    private void authenticate(Long userId) {
        CustomUserDetails user = new CustomUserDetails(new AuthorizationSnapshot(
                userId,
                3L,
                "student@example.test",
                UserStatus.ACTIVE,
                null,
                0,
                List.of("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()));
    }
}
