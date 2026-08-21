package com.collegeerp.erp.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.collegeerp.erp.auth.security.AuthorizationSnapshot;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.auth.security.TrustedClientIpResolver;
import com.collegeerp.erp.auth.service.DistributedRateLimiter;
import com.collegeerp.erp.config.RateLimitProperties;
import com.collegeerp.erp.user.entity.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginRateLimitFilterTest {
    private final DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<DistributedRateLimiter> limiterProvider = mock(ObjectProvider.class);
    private final TrustedClientIpResolver clientIps = mock(TrustedClientIpResolver.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TrustedClientIpResolver> clientIpProvider = mock(ObjectProvider.class);
    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        when(limiterProvider.getIfAvailable()).thenReturn(limiter);
        when(clientIpProvider.getIfAvailable()).thenReturn(clientIps);
        when(limiter.check(anyString(), anyString(), anyLong(), any(Duration.class)))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));
        when(limiter.checkBackoff(anyString(), anyString()))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));
        filter = new LoginRateLimitFilter(
                limiterProvider,
                clientIpProvider,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new RateLimitProperties(),
                true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedApiTrafficUsesLooserUserPolicy() throws Exception {
        authenticate(42L);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notices/unread-count");
        when(clientIps.resolve(request)).thenReturn("203.0.113.7");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(limiter).check(
                eq("http:authenticated-user"), eq("42"), eq(600L), any(Duration.class));
    }

    @Test
    void publicApiTrafficUsesModerateIpPolicy() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/public/admissions/options");
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(limiter).check(
                eq("http:public-ip"), eq("198.51.100.40"), eq(120L), any(Duration.class));
    }

    @Test
    void loginUsesBothIpAndNormalizedAccountPolicies() throws Exception {
        MockHttpServletRequest request = loginRequest(" Student@Example.com ");
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(limiter).check(eq("http:auth:login:ip"), eq("198.51.100.40"),
                eq(20L), any(Duration.class));
        verify(limiter).check(eq("http:auth:login:account"), eq("student@example.com"),
                eq(6L), any(Duration.class));
        verify(limiter, never()).checkBackoff(
                "http:auth:login:backoff-account", "student@example.com");
    }

    @Test
    void publicAdmissionAllowsTenAttemptsPerIpAndEmail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/public/admissions/college/ABC001/submit");
        request.setContentType("application/json");
        request.setContent("{\"email\":\" Student@Example.com \"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(limiter).check(eq("http:auth:signup:ip"), eq("198.51.100.40"),
                eq(10L), any(Duration.class));
        verify(limiter).check(eq("http:auth:signup:account"), eq("student@example.com"),
                eq(10L), any(Duration.class));
    }

    @Test
    void failedLoginDoesNotApplyImmediateExponentialBackoff() throws Exception {
        MockHttpServletRequest request = loginRequest("student@example.com");
        when(clientIps.resolve(request)).thenReturn("198.51.100.40");
        FilterChain failingChain = mock(FilterChain.class);
        doAnswer(invocation -> {
            ((MockHttpServletResponse) invocation.getArgument(1)).setStatus(401);
            return null;
        }).when(failingChain).doFilter(any(), any());

        filter.doFilter(request, new MockHttpServletResponse(), failingChain);

        verify(limiter, never()).recordFailure(eq("http:auth:login:backoff-ip"),
                eq("198.51.100.40"), any(), any(), any());
        verify(limiter, never()).recordFailure(eq("http:auth:login:backoff-account"),
                eq("student@example.com"), any(), any(), any());
    }

    @Test
    void seventhLoginAttemptReturnsTooManyAuthenticationAttempts() throws Exception {
        when(limiter.check(eq("http:auth:login:account"), eq("student@example.com"),
                eq(6L), any(Duration.class)))
                .thenReturn(
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(true, 0),
                        new DistributedRateLimiter.Decision(false, 30));
        FilterChain failingChain = mock(FilterChain.class);
        doAnswer(invocation -> {
            ((MockHttpServletResponse) invocation.getArgument(1)).setStatus(401);
            return null;
        }).when(failingChain).doFilter(any(), any());

        for (int attempt = 1; attempt <= 6; attempt++) {
            MockHttpServletRequest request = loginRequest("student@example.com");
            when(clientIps.resolve(request)).thenReturn("198.51.100.40");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, failingChain);

            assertThat(response.getStatus()).isEqualTo(401);
        }

        MockHttpServletRequest seventh = loginRequest("student@example.com");
        when(clientIps.resolve(seventh)).thenReturn("198.51.100.40");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(seventh, response, failingChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("Too many authentication attempts");
    }

    private MockHttpServletRequest loginRequest(String email) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent(("{\"email\":\"" + email + "\",\"password\":\"Secret1!\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }

    private void authenticate(Long userId) {
        CustomUserDetails user = new CustomUserDetails(new AuthorizationSnapshot(
                userId, 3L, "student@example.test", UserStatus.ACTIVE,
                null, 0, List.of("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
