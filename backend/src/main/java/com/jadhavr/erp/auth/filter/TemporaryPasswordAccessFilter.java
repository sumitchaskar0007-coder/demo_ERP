package com.jadhavr.erp.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.common.api.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Enforces temporary-password restrictions on the server for every authenticated role. */
@Component
public class TemporaryPasswordAccessFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/account/change-password",
            "/api/v1/auth/me",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/csrf");

    private final ObjectMapper objectMapper;

    public TemporaryPasswordAccessFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/auth/password/")
                || path.startsWith("/api/auth/email-verification/")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/health")
                || path.startsWith("/actuator/health")
                || ALLOWED_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails user
                && user.isMustChangePassword()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                    "Change your temporary password before accessing application features",
                    request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }
}
