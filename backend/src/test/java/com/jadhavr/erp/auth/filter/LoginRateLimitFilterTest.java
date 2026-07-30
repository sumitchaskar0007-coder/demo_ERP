package com.jadhavr.erp.auth.filter;

import com.jadhavr.erp.auth.security.AuthorizationSnapshot;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import com.jadhavr.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void authenticatedTrafficUsesUserIdInsteadOfSharedIp() throws Exception {
        DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        @SuppressWarnings("unchecked")
        ObjectProvider<DistributedRateLimiter> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(limiter);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(provider, 1000, 1000, 600, 300);
        CustomUserDetails user = new CustomUserDetails(new AuthorizationSnapshot(
                42L, 3L, "student@example.test", "Student", UserStatus.ACTIVE,
                null, 0, false, List.of("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notices/unread-count");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(jakarta.servlet.FilterChain.class));

        verify(limiter).tryAcquire(eq("http:api-user"), eq("42"), eq(600L), any());
    }

    @Test
    void rejectedRequestReturnsConsistent429AndRetryAfter() throws Exception {
        DistributedRateLimiter limiter = mock(DistributedRateLimiter.class);
        when(limiter.tryAcquire(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        @SuppressWarnings("unchecked")
        ObjectProvider<DistributedRateLimiter> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(limiter);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(provider, 1000, 1000, 600, 300);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(jakarta.servlet.FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("Too many requests");
    }
}
