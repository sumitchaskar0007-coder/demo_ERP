package com.jadhavr.erp.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectProvider<AuthorizationSnapshotService> authorizationSnapshots;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            ObjectProvider<AuthorizationSnapshotService> authorizationSnapshots) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authorizationSnapshots = authorizationSnapshots;
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
                AuthorizationSnapshotService snapshots = authorizationSnapshots.getIfAvailable();
                UserDetails user = snapshots == null
                        ? userDetailsService.loadUserByUsername(email)
                        : snapshots.load(jwtService.extractUserId(token), email);
                if (jwtService.isTokenValid(token, user)) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            // Continue unauthenticated so the public refresh endpoint can rotate an
            // expired access token. Protected endpoints are rejected by Spring Security.
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
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
