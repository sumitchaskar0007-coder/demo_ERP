package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.common.api.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final AuthorizationSnapshotService authorizationSnapshots;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
            AuthorizationSnapshotService authorizationSnapshots,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.authorizationSnapshots = authorizationSnapshots;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = accessTokenCookie(request);
        // Optional bearer support is retained for trusted non-browser API clients.
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (token == null && header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String email = jwtService.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = authorizationSnapshots.load(
                        jwtService.extractUserId(token), email);
                if (jwtService.isTokenValid(token, user)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
            // Continue unauthenticated so the public refresh endpoint can rotate an
            // expired access token. Protected endpoints are rejected by Spring Security.
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            log.error("Authentication data store unavailable method={} path={}",
                    request.getMethod(), request.getRequestURI(), exception);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    new ErrorResponse("Authentication service is temporarily unavailable",
                            request.getRequestURI()));
        }
    }

    private String accessTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (AuthCookieService.ACCESS_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
