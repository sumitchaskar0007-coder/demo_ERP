package com.jadhavr.erp.auth.filter;

import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final ObjectProvider<DistributedRateLimiter> limiter;
    private final long loginIpLimit;
    private final long refreshIpLimit;
    private final long authenticatedUserLimit;
    private final long anonymousIpLimit;

    public LoginRateLimitFilter(ObjectProvider<DistributedRateLimiter> limiter,
            @Value("${app.rate-limit.login-ip-per-minute:1000}") long loginIpLimit,
            @Value("${app.rate-limit.refresh-ip-per-minute:1000}") long refreshIpLimit,
            @Value("${app.rate-limit.api-user-per-minute:600}") long authenticatedUserLimit,
            @Value("${app.rate-limit.api-anonymous-ip-per-minute:300}") long anonymousIpLimit) {
        this.limiter = limiter;
        this.loginIpLimit = loginIpLimit;
        this.refreshIpLimit = refreshIpLimit;
        this.authenticatedUserLimit = authenticatedUserLimit;
        this.anonymousIpLimit = anonymousIpLimit;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        DistributedRateLimiter rateLimiter = limiter.getIfAvailable();
        if (rateLimiter == null) {
            chain.doFilter(request, response);
            return;
        }
        RequestLimit limit = limitFor(request);
        boolean allowed = rateLimiter.tryAcquire(limit.namespace(), limit.subject(),
                limit.requests(), Duration.ofMinutes(1));
        if (!allowed) {
            response.setStatus(429); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests\",\"timestamp\":\""
                    + LocalDateTime.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private RequestLimit limitFor(HttpServletRequest request) {
        String ip = request.getRemoteAddr(); // RemoteIpValve accepts forwarding headers only from trusted proxies.
        if ("POST".equals(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI())) {
            return new RequestLimit("http:login-ip", ip, loginIpLimit);
        }
        if ("POST".equals(request.getMethod()) && "/api/v1/auth/refresh".equals(request.getRequestURI())) {
            return new RequestLimit("http:refresh-ip", ip, refreshIpLimit);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof com.jadhavr.erp.auth.security.CustomUserDetails user) {
            return new RequestLimit("http:api-user", Long.toString(user.getId()), authenticatedUserLimit);
        }
        return new RequestLimit("http:api-anonymous-ip", ip, anonymousIpLimit);
    }

    private record RequestLimit(String namespace, String subject, long requests) {}
}
