package com.jadhavr.erp.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class AuthorizationSnapshotService {
    private static final DefaultRedisScript<Long> BEGIN_INVALIDATION =
            new DefaultRedisScript<>("""
                    redis.call('PSETEX', KEYS[2], ARGV[1], '1')
                    redis.call('DEL', KEYS[1])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> WRITE_IF_NOT_INVALIDATING =
            new DefaultRedisScript<>("""
                    if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
                    redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[1])
                    return 1
                    """, Long.class);

    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final boolean redisEnabled;
    private final Duration ttl;
    private final Duration invalidationGuardTtl;
    private final String prefix;
    private final Counter hits;
    private final Counter misses;
    private final Counter fallbacks;
    private final Counter invalidationFailures;

    public AuthorizationSnapshotService(
            UserRepository users,
            StringRedisTemplate redis,
            ObjectMapper json,
            MeterRegistry metrics,
            @Value("${app.auth.authorization-cache-enabled:${app.rate-limit.redis-enabled:false}}")
            boolean redisEnabled,
            @Value("${app.auth.authorization-cache-ttl-seconds:30}") long ttlSeconds,
            @Value("${app.cache.environment:local}") String environment) {
        this.users = users;
        this.redis = redis;
        this.json = json;
        this.redisEnabled = redisEnabled;
        this.ttl = Duration.ofSeconds(Math.max(5, ttlSeconds));
        this.invalidationGuardTtl = this.ttl.plusSeconds(10);
        this.prefix = "college-erp:" + environment + ":authz:";
        this.hits = metrics.counter("auth.authorization.snapshot", "result", "hit");
        this.misses = metrics.counter("auth.authorization.snapshot", "result", "miss");
        this.fallbacks = metrics.counter("auth.authorization.snapshot", "result", "redis-fallback");
        this.invalidationFailures = metrics.counter(
                "auth.authorization.snapshot", "result", "invalidation-failure");
    }

    public CustomUserDetails load(Long userId, String expectedEmail) {
        if (userId == null || expectedEmail == null || expectedEmail.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        boolean cachePermitted = redisEnabled;
        if (redisEnabled) {
            try {
                if (Boolean.TRUE.equals(redis.hasKey(guardKey(userId)))) {
                    misses.increment();
                    cachePermitted = false;
                } else {
                    String value = redis.opsForValue().get(snapshotKey(userId));
                    if (value != null) {
                        AuthorizationSnapshot snapshot =
                                json.readValue(value, AuthorizationSnapshot.class);
                        if (matches(snapshot, userId, expectedEmail)) {
                            hits.increment();
                            return new CustomUserDetails(snapshot);
                        }
                        redis.delete(snapshotKey(userId));
                    }
                    misses.increment();
                }
            } catch (Exception exception) {
                fallbacks.increment();
                cachePermitted = false;
            }
        }

        User user = users.findAuthorizationById(userId)
                .filter(item -> expectedEmail.equalsIgnoreCase(item.getEmail()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        CustomUserDetails details = new CustomUserDetails(user);
        if (cachePermitted) {
            cache(details);
        }
        return details;
    }

    /**
     * Starts a fail-safe invalidation before commit. A guard prevents another
     * request from repopulating the old database state before the transaction
     * commits. If Redis cannot establish the guard, the caller's transaction
     * fails instead of leaving stale authorization cached.
     */
    public void invalidateOrThrow(Long userId) {
        if (!redisEnabled || userId == null) {
            return;
        }
        try {
            Long result = redis.execute(
                    BEGIN_INVALIDATION,
                    List.of(snapshotKey(userId), guardKey(userId)),
                    Long.toString(invalidationGuardTtl.toMillis()));
            if (result == null) {
                throw new IllegalStateException("Redis returned no invalidation result");
            }
        } catch (RuntimeException exception) {
            invalidationFailures.increment();
            throw new AuthorizationStateUnavailableException(
                    "Authorization state is temporarily unavailable", exception);
        }

        // The guard intentionally remains until its TTL expires. It is longer
        // than the snapshot TTL, so an in-flight read of pre-commit database
        // state cannot repopulate a snapshot after the mutation commits.
    }

    private void cache(CustomUserDetails details) {
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                details.getId(),
                details.getCollegeId(),
                details.getUsername(),
                details.getStatus(),
                details.getLockedUntil(),
                details.getSessionVersion(),
                details.getAuthorities().stream().map(Object::toString).toList());
        try {
            redis.execute(
                    WRITE_IF_NOT_INVALIDATING,
                    List.of(snapshotKey(details.getId()), guardKey(details.getId())),
                    json.writeValueAsString(snapshot),
                    Long.toString(ttl.toMillis()));
        } catch (Exception exception) {
            fallbacks.increment();
        }
    }

    private boolean matches(AuthorizationSnapshot snapshot, Long userId, String expectedEmail) {
        return snapshot != null
                && userId.equals(snapshot.userId())
                && snapshot.email() != null
                && expectedEmail.equalsIgnoreCase(snapshot.email())
                && snapshot.status() != null
                && snapshot.authorities() != null;
    }

    private String snapshotKey(Long userId) {
        return prefix + userId;
    }

    private String guardKey(Long userId) {
        return prefix + "invalidating:" + userId;
    }
}
