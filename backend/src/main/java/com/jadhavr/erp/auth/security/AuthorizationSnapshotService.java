package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthorizationSnapshotService {
    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final boolean redisEnabled;
    private final Duration ttl;
    private final String prefix;
    private final Counter hits;
    private final Counter misses;
    private final Counter fallbacks;

    public AuthorizationSnapshotService(
            UserRepository users,
            StringRedisTemplate redis,
            ObjectMapper json,
            MeterRegistry metrics,
            @Value("${app.auth.authorization-cache-enabled:true}") boolean redisEnabled,
            @Value("${app.auth.authorization-cache-ttl-seconds:60}") long ttlSeconds,
            @Value("${app.cache.environment:local}") String environment) {
        this.users = users;
        this.redis = redis;
        this.json = json;
        this.redisEnabled = redisEnabled;
        this.ttl = Duration.ofSeconds(Math.max(5, ttlSeconds));
        this.prefix = "college-erp:" + environment + ":authz:";
        this.hits = metrics.counter("auth.authorization.snapshot", "result", "hit");
        this.misses = metrics.counter("auth.authorization.snapshot", "result", "miss");
        this.fallbacks = metrics.counter("auth.authorization.snapshot", "result", "redis-fallback");
    }

    public CustomUserDetails load(Long userId, String expectedEmail) {
        String key = key(userId);
        if (redisEnabled) {
            try {
                String value = redis.opsForValue().get(key);
                if (value != null) {
                    AuthorizationSnapshot snapshot = json.readValue(value, AuthorizationSnapshot.class);
                    if (expectedEmail.equalsIgnoreCase(snapshot.email())) {
                        hits.increment();
                        return new CustomUserDetails(snapshot);
                    }
                    redis.delete(key);
                }
                misses.increment();
            } catch (Exception ignored) {
                fallbacks.increment();
            }
        }
        User user = users.findById(userId)
                .filter(item -> expectedEmail.equalsIgnoreCase(item.getEmail()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        CustomUserDetails details = new CustomUserDetails(user);
        cache(details);
        return details;
    }

    public void evict(Long userId) {
        if (!redisEnabled || userId == null) return;
        try {
            redis.delete(key(userId));
        } catch (RuntimeException ignored) {
            fallbacks.increment();
        }
    }

    private void cache(CustomUserDetails details) {
        if (!redisEnabled) return;
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                details.getId(), details.getCollegeId(), details.getUsername(), details.getFullName(),
                details.getStatus(), details.getLockedUntil(), details.getSessionVersion(),
                details.isMustChangePassword(),
                details.getAuthorities().stream().map(Object::toString).toList());
        try {
            redis.opsForValue().set(key(details.getId()), json.writeValueAsString(snapshot), ttl);
        } catch (Exception ignored) {
            fallbacks.increment();
        }
    }

    private String key(Long userId) {
        return prefix + userId;
    }
}
