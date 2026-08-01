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
    private static final DefaultRedisScript<Long> RECORD_FAILURE = new DefaultRedisScript<>("""
            local time = redis.call('TIME')
            local now = (time[1] * 1000) + math.floor(time[2] / 1000)
            local failures = redis.call('HINCRBY', KEYS[1], 'failures', 1)
            local delay = tonumber(ARGV[2]) * (2 ^ math.min(failures - 1, 30))
            delay = math.min(delay, tonumber(ARGV[3]))
            redis.call('HSET', KEYS[1], 'blockedUntil', now + delay)
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
            return delay
            """, Long.class);
    private static final DefaultRedisScript<Long> CHECK_BACKOFF = new DefaultRedisScript<>("""
            local blockedUntil = redis.call('HGET', KEYS[1], 'blockedUntil')
            if not blockedUntil then return 0 end
            local time = redis.call('TIME')
            local now = (time[1] * 1000) + math.floor(time[2] / 1000)
            return math.max(0, tonumber(blockedUntil) - now)
            """, Long.class);

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final boolean failClosed;
    private final byte[] keySecret;
    private final Cache<String, LocalWindow> localCounters = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(24))
            .build();
    private final Cache<String, LocalBackoff> localBackoffs = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(24))
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
        String key = rateKey(namespace, subject);
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

    public Decision checkBackoff(String namespace, String subject) {
        String key = rateKey(namespace, subject);
        if (!redisEnabled) {
            LocalBackoff state = localBackoffs.getIfPresent(key);
            if (state == null) return Decision.allowedDecision();
            long remaining = state.blockedUntilMillis - System.currentTimeMillis();
            return remaining > 0 ? Decision.rejected(retrySeconds(remaining))
                    : Decision.allowedDecision();
        }
        try {
            Long remaining = redis.execute(CHECK_BACKOFF, List.of(key));
            return remaining != null && remaining > 0
                    ? Decision.rejected(retrySeconds(remaining))
                    : Decision.allowedDecision();
        } catch (RuntimeException exception) {
            log.warn("rate_limit_backoff_store_unavailable namespace={} failClosed={}",
                    safeNamespace(namespace), failClosed);
            return failClosed ? Decision.rejected(1) : Decision.allowedDecision();
        }
    }

    public long recordFailure(String namespace, String subject, Duration baseDelay,
            Duration maxDelay, Duration resetAfter) {
        validateBackoff(baseDelay, maxDelay, resetAfter);
        String key = rateKey(namespace, subject);
        if (!redisEnabled) {
            long now = System.currentTimeMillis();
            LocalBackoff state = localBackoffs.asMap().compute(key, (ignored, existing) -> {
                long failures = existing == null || now >= existing.resetsAtMillis
                        ? 1 : existing.failures + 1;
                long delay = exponentialDelay(
                        failures, baseDelay.toMillis(), maxDelay.toMillis());
                return new LocalBackoff(failures, now + delay, now + resetAfter.toMillis());
            });
            return retrySeconds(Math.max(1, state.blockedUntilMillis - now));
        }
        try {
            Long delay = redis.execute(
                    RECORD_FAILURE,
                    List.of(key),
                    Long.toString(resetAfter.toMillis()),
                    Long.toString(baseDelay.toMillis()),
                    Long.toString(maxDelay.toMillis()));
            if (delay == null) throw new IllegalStateException("Redis returned no backoff result");
            return retrySeconds(delay);
        } catch (RuntimeException exception) {
            log.warn("rate_limit_backoff_store_unavailable namespace={} failClosed={}",
                    safeNamespace(namespace), failClosed);
            return failClosed ? Math.max(1, baseDelay.toSeconds()) : 0;
        }
    }

    public void resetBackoff(String namespace, String subject) {
        String key = rateKey(namespace, subject);
        if (!redisEnabled) {
            localBackoffs.invalidate(key);
            return;
        }
        try {
            redis.delete(key);
        } catch (RuntimeException exception) {
            log.warn("rate_limit_backoff_reset_failed namespace={}", safeNamespace(namespace));
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

    private String rateKey(String namespace, String subject) {
        return "erp:rate:" + safeNamespace(namespace) + ":" + pseudonym(subject);
    }

    private void validateBackoff(Duration baseDelay, Duration maxDelay, Duration resetAfter) {
        if (baseDelay == null || maxDelay == null || resetAfter == null
                || baseDelay.isZero() || baseDelay.isNegative()
                || maxDelay.isZero() || maxDelay.isNegative()
                || resetAfter.isZero() || resetAfter.isNegative()
                || baseDelay.compareTo(maxDelay) > 0) {
            throw new IllegalArgumentException("Rate-limit backoff durations are invalid");
        }
    }

    private long exponentialDelay(long failures, long baseMillis, long maxMillis) {
        int exponent = (int) Math.min(Math.max(0, failures - 1), 30);
        long multiplier = 1L << exponent;
        if (baseMillis > maxMillis / multiplier) return maxMillis;
        return Math.min(maxMillis, baseMillis * multiplier);
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

    private record LocalBackoff(long failures, long blockedUntilMillis, long resetsAtMillis) {
    }
}
