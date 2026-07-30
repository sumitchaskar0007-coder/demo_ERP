package com.jadhavr.erp.notice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NoticeStreamService {
    public static final String CHANNEL = "college-erp:notice-events";
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final long timeoutMs;

    public NoticeStreamService(StringRedisTemplate redis,
            @Value("${app.rate-limit.redis-enabled:false}") boolean redisEnabled,
            @Value("${app.notice.stream-timeout-ms:1800000}") long timeoutMs) {
        this.redis = redis;
        this.redisEnabled = redisEnabled;
        this.timeoutMs = timeoutMs;
    }

    public SseEmitter subscribe(Long userId, long unreadCount) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        send(userId, emitter, "unread-count", Long.toString(unreadCount));
        return emitter;
    }

    public void publishAfterCommit() {
        Runnable publish = this::publish;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }

    public void onSharedEvent(String ignored) {
        broadcast("refresh", "changed");
    }

    @Scheduled(fixedDelayString = "${app.notice.heartbeat-ms:25000}")
    public void heartbeat() {
        broadcast("heartbeat", Long.toString(System.currentTimeMillis()));
    }

    private void publish() {
        if (redisEnabled) {
            try {
                redis.convertAndSend(CHANNEL, "changed");
                return;
            } catch (RuntimeException ignored) {
                // Local delivery keeps the current task functional during a transient Redis failure.
            }
        }
        onSharedEvent("changed");
    }

    private void broadcast(String event, String data) {
        emitters.forEach((userId, subscribers) ->
                subscribers.forEach(emitter -> send(userId, emitter, event, data)));
    }

    private void send(Long userId, SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data).reconnectTime(3000));
        } catch (IOException | IllegalStateException exception) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> subscribers = emitters.get(userId);
        if (subscribers == null) return;
        subscribers.remove(emitter);
        if (subscribers.isEmpty()) emitters.remove(userId, subscribers);
    }
}
