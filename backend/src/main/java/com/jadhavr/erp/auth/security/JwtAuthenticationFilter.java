package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.common.api.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
            CustomUserDetailsService userDetailsService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.startsWith("/api/public/")
                || path.equals("/actuator/health")
                || path.equals("/api/health")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
            String email = jwtService.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(token, user)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    if (user instanceof CustomUserDetails details
                            && details.isMustChangePassword()
                            && !request.getRequestURI().equals("/api/auth/change-password")
                            && !request.getRequestURI().equals("/api/auth/profile")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getOutputStream(),
                                new ErrorResponse("Password change required before using the platform"));
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    new ErrorResponse("Invalid or expired token"));
        }
    }
}
