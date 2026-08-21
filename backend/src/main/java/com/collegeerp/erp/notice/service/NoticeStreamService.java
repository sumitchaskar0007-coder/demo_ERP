package com.collegeerp.erp.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NoticeStreamService {
    static final String CHANNEL = "college-erp:notice-events:v1";

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Registration>> registrations =
            new ConcurrentHashMap<>();
    private final AtomicInteger activeConnections = new AtomicInteger();
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final TaskExecutor executor;
    private final boolean redisEnabled;
    private final long timeoutMs;
    private final int maxConnections;
    private final int maxConnectionsPerUser;
    private final Counter published;
    private final Counter sendFailures;
    private final Counter dropped;

    public NoticeStreamService(
            StringRedisTemplate redis,
            ObjectMapper json,
            @Qualifier("noticeStreamExecutor") TaskExecutor executor,
            MeterRegistry metrics,
            @Value("${app.notice.redis-enabled:false}") boolean redisEnabled,
            @Value("${app.notice.stream-timeout-ms:1800000}") long timeoutMs,
            @Value("${app.notice.max-connections:10000}") int maxConnections,
            @Value("${app.notice.max-connections-per-user:3}") int maxConnectionsPerUser) {
        this.redis = redis;
        this.json = json;
        this.executor = executor;
        this.redisEnabled = redisEnabled;
        if (timeoutMs < 1 || maxConnections < 1 || maxConnectionsPerUser < 1) {
            throw new IllegalArgumentException("Notice stream limits must be positive");
        }
        this.timeoutMs = timeoutMs;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerUser = maxConnectionsPerUser;
        this.published = metrics.counter("notice.stream.events", "result", "published");
        this.sendFailures = metrics.counter("notice.stream.events", "result", "send-failure");
        this.dropped = metrics.counter("notice.stream.events", "result", "executor-saturated");
        Gauge.builder("notice.stream.connections", activeConnections, AtomicInteger::get)
                .register(metrics);
    }

    public SseEmitter subscribe(NoticeStreamSubscriber subscriber, long unreadCount) {
        CopyOnWriteArrayList<Registration> userRegistrations =
                registrations.computeIfAbsent(subscriber.userId(), ignored -> new CopyOnWriteArrayList<>());
        if (activeConnections.get() >= maxConnections
                || userRegistrations.size() >= maxConnectionsPerUser) {
            if (userRegistrations.isEmpty()) registrations.remove(subscriber.userId(), userRegistrations);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many notice stream connections");
        }

        SseEmitter emitter = new SseEmitter(timeoutMs);
        register(subscriber, emitter);
        sendNow(new Registration(subscriber, emitter), "unread-count", Long.toString(unreadCount));
        return emitter;
    }

    void register(NoticeStreamSubscriber subscriber, SseEmitter emitter) {
        Registration registration = new Registration(subscriber, emitter);
        registrations.computeIfAbsent(subscriber.userId(), ignored -> new CopyOnWriteArrayList<>())
                .add(registration);
        activeConnections.incrementAndGet();
        Runnable cleanup = () -> remove(registration);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
    }

    public void publishAfterCommit(NoticeStreamEvent event) {
        Runnable action = () -> publish(event);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    public void onSharedEvent(String payload) {
        try {
            deliver(json.readValue(payload, NoticeStreamEvent.class));
        } catch (IOException exception) {
            dropped.increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.notice.heartbeat-ms:25000}")
    public void heartbeat() {
        registrations.values().forEach(items ->
                items.forEach(registration -> sendAsync(registration, "heartbeat", "ok")));
    }

    void deliver(NoticeStreamEvent event) {
        registrations.values().forEach(items -> items.stream()
                .filter(registration -> event.matches(registration.subscriber()))
                .forEach(registration -> sendAsync(
                        registration, "notice-change", serialize(event))));
    }

    private void publish(NoticeStreamEvent event) {
        String payload = serialize(event);
        if (redisEnabled) {
            try {
                redis.convertAndSend(CHANNEL, payload);
                published.increment();
                return;
            } catch (RuntimeException ignored) {
                // Deliver on this replica; the count-only fallback heals other replicas.
            }
        }
        deliver(event);
        published.increment();
    }

    private String serialize(NoticeStreamEvent event) {
        try {
            return json.writeValueAsString(event);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize notice event", exception);
        }
    }

    private void sendAsync(Registration registration, String event, String data) {
        try {
            executor.execute(() -> sendNow(registration, event, data));
        } catch (TaskRejectedException exception) {
            dropped.increment();
        }
    }

    private void sendNow(Registration registration, String event, String data) {
        try {
            registration.emitter().send(SseEmitter.event()
                    .name(event)
                    .data(data)
                    .reconnectTime(3_000));
        } catch (IOException | IllegalStateException exception) {
            sendFailures.increment();
            remove(registration);
        }
    }

    private void remove(Registration registration) {
        CopyOnWriteArrayList<Registration> userRegistrations =
                registrations.get(registration.subscriber().userId());
        if (userRegistrations == null || !userRegistrations.remove(registration)) return;
        activeConnections.decrementAndGet();
        if (userRegistrations.isEmpty()) {
            registrations.remove(registration.subscriber().userId(), userRegistrations);
        }
    }

    private record Registration(NoticeStreamSubscriber subscriber, SseEmitter emitter) {
    }
}
