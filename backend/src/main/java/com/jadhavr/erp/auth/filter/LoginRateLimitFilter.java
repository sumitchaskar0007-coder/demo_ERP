package com.jadhavr.erp.auth.filter;

import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final ObjectProvider<DistributedRateLimiter> limiter;
    public LoginRateLimitFilter(ObjectProvider<DistributedRateLimiter> limiter) {
        this.limiter = limiter;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        DistributedRateLimiter rateLimiter = limiter.getIfAvailable();
        if (rateLimiter == null) {
            chain.doFilter(request, response);
            return;
        }
        boolean allowed = rateLimiter.tryAcquire("http:" + limit.name(), request.getRemoteAddr(),
                limit.requests, Duration.ofMinutes(1));
        if (!allowed) {
            response.setStatus(429); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests\",\"timestamp\":\""
                    + LocalDateTime.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
    private Limit limitFor(HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) && "/api/v1/auth/login".equals(request.getRequestURI())) return Limit.LOGIN;
        if ("POST".equals(request.getMethod()) && "/api/v1/auth/refresh".equals(request.getRequestURI())) return Limit.REFRESH;
        return Limit.API;
    }
    private enum Limit {
        LOGIN(10), REFRESH(30), API(300);
        private final int requests;
        Limit(int requests) { this.requests = requests; }
    }
}
