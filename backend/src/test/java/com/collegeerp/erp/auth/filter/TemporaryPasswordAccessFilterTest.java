package com.collegeerp.erp.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.collegeerp.erp.auth.security.AuthorizationSnapshot;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.user.entity.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TemporaryPasswordAccessFilterTest {
    private final TemporaryPasswordAccessFilter filter =
            new TemporaryPasswordAccessFilter(new ObjectMapper().registerModule(new JavaTimeModule()));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksApplicationApisForEveryRoleWhilePasswordIsTemporary() throws Exception {
        authenticate(true, "ROLE_PRINCIPAL");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/principal/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Change your temporary password");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsPasswordChangeAndNormalUsers() throws Exception {
        authenticate(true, "ROLE_STUDENT");
        MockHttpServletRequest change = new MockHttpServletRequest("PATCH", "/api/account/change-password");
        MockHttpServletResponse changeResponse = new MockHttpServletResponse();
        FilterChain changeChain = mock(FilterChain.class);
        filter.doFilter(change, changeResponse, changeChain);
        verify(changeChain).doFilter(change, changeResponse);

        authenticate(false, "ROLE_PRINCIPAL");
        MockHttpServletRequest normal = new MockHttpServletRequest("GET", "/api/principal/dashboard");
        MockHttpServletResponse normalResponse = new MockHttpServletResponse();
        FilterChain normalChain = mock(FilterChain.class);
        filter.doFilter(normal, normalResponse, normalChain);
        verify(normalChain).doFilter(normal, normalResponse);
    }

    private void authenticate(boolean mustChangePassword, String role) {
        CustomUserDetails user = new CustomUserDetails(new AuthorizationSnapshot(
                7L, 2L, "user@example.test", UserStatus.ACTIVE, null, 1,
                mustChangePassword, List.of(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
