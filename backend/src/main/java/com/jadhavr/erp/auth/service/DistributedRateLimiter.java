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
import java.util.concurrent.atomic.AtomicLong;

/** Shared, fixed-window limiter. Production uses Redis; the bounded cache is for local development only. */
@Service
public class DistributedRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiter.class);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final boolean failClosed;
    private final byte[] keySecret;
    private final Cache<String, AtomicLong> localCounters = Caffeine.newBuilder()
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
        String key = "erp:rate:" + safeNamespace(namespace) + ":" + pseudonym(subject);
        if (!redisEnabled) {
            return localCounters.get(key, ignored -> new AtomicLong()).incrementAndGet() <= maximum;
        }
        try {
            Long current = redis.execute(INCREMENT_WITH_TTL, List.of(key), Long.toString(ttl.toMillis()));
            return current != null && current <= maximum;
        } catch (RuntimeException exception) {
            log.warn("rate_limit_store_unavailable namespace={} failClosed={}", safeNamespace(namespace), failClosed);
            return !failClosed;
        }
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
}
