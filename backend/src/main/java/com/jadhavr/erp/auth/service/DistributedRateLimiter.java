package com.jadhavr.erp.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/** Shared fixed-window limiter. Production uses Redis; the bounded cache is local-development fallback only. */
@Service
public class DistributedRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiter.class);
    private static final DefaultRedisScript<Long> CHECK_LIMIT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            if current > tonumber(ARGV[2]) then
                local remaining = redis.call('PTTL', KEYS[1])
                if remaining < 1 then remaining = tonumber(ARGV[1]) end
                return remaining
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final boolean failClosed;
    private final byte[] keySecret;
    private final Cache<String, LocalWindow> localCounters = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();

    public DistributedRateLimiter(StringRedisTemplate redis,
            @Value("${app.rate-limit.redis-enabled:false}") boolean redisEnabled,
            @Value("${app.rate-limit.fail-closed:false}") boolean failClosed,
            @Value("${app.rate-limit.key-secret:local-rate-limit-key}") String keySecret) {
        this.redis = redis;
        this.redisEnabled = redisEnabled;
        this.failClosed = failClosed;
        this.keySecret = keySecret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean tryAcquire(String namespace, String subject, long maximum, Duration ttl) {
        return check(namespace, subject, maximum, ttl).allowed();
    }

    public Decision check(String namespace, String subject, long maximum, Duration ttl) {
        if (maximum < 1 || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Rate-limit maximum and window must be positive");
        }
        String key = "erp:rate:" + safeNamespace(namespace) + ":" + pseudonym(subject);
        if (!redisEnabled) {
            return localDecision(key, maximum, ttl);
        }
        try {
            Long retryAfterMillis = redis.execute(
                    CHECK_LIMIT,
                    List.of(key),
                    Long.toString(ttl.toMillis()),
                    Long.toString(maximum));
            if (retryAfterMillis == null) {
                throw new IllegalStateException("Redis returned no rate-limit result");
            }
            return retryAfterMillis == 0
                    ? Decision.allowedDecision()
                    : Decision.rejected(retrySeconds(retryAfterMillis));
        } catch (RuntimeException exception) {
            log.warn("rate_limit_store_unavailable namespace={} failClosed={}", safeNamespace(namespace), failClosed);
            return failClosed
                    ? Decision.rejected(Math.max(1, ttl.toSeconds()))
                    : Decision.allowedDecision();
        }
    }

    private Decision localDecision(String key, long maximum, Duration ttl) {
        long now = System.nanoTime();
        LocalWindow window = localCounters.asMap().compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.expiresAtNanos) {
                return new LocalWindow(1, now + ttl.toNanos());
            }
            existing.count++;
            return existing;
        });
        if (window.count <= maximum) {
            return Decision.allowedDecision();
        }
        long remainingNanos = Math.max(1, window.expiresAtNanos - now);
        return Decision.rejected(Math.max(1,
                (remainingNanos + 999_999_999L) / 1_000_000_000L));
    }

    private long retrySeconds(long milliseconds) {
        return Math.max(1, (milliseconds + 999) / 1000);
    }

    private String pseudonym(String subject) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(subject.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create rate-limit key", exception);
        }
    }

    private String safeNamespace(String value) {
        return value.replaceAll("[^a-zA-Z0-9:_-]", "_");
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
        private static Decision allowedDecision() {
            return new Decision(true, 0);
        }

        private static Decision rejected(long retryAfterSeconds) {
            return new Decision(false, Math.max(1, retryAfterSeconds));
        }
    }

    private static final class LocalWindow {
        private long count;
        private final long expiresAtNanos;

        private LocalWindow(long count, long expiresAtNanos) {
            this.count = count;
            this.expiresAtNanos = expiresAtNanos;
        }
    }
}
