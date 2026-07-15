package com.jadhavr.erp.auth.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Bucket> localBuckets = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    public LoginRateLimitFilter(StringRedisTemplate redis, @Value("${app.rate-limit.redis-enabled:false}") boolean redisEnabled) {
        this.redis = redis; this.redisEnabled = redisEnabled;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = limitFor(request);
        String key = "erp:api-rate:" + limit.name() + ":" + clientIp(request);
        boolean allowed = redisEnabled
                ? consumeRedis(key, limit)
                : localBuckets.computeIfAbsent(key, ignored -> newBucket(limit)).tryConsume(1);
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
    private Bucket newBucket(Limit limit) {
        return Bucket.builder().addLimit(Bandwidth.classic(limit.requests,
                Refill.intervally(limit.requests, Duration.ofMinutes(1)))).build();
    }
    private boolean consumeRedis(String key, Limit limit) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, Duration.ofMinutes(1));
        return count != null && count <= limit.requests;
    }
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
    private enum Limit {
        LOGIN(10), REFRESH(30), API(300);
        private final int requests;
        Limit(int requests) { this.requests = requests; }
    }
}
