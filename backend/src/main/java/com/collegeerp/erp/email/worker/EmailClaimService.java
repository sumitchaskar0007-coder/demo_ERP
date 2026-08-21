package com.collegeerp.erp.email.worker;

import com.collegeerp.erp.email.entity.EmailNotification;
import com.collegeerp.erp.email.enums.EmailStatus;
import com.collegeerp.erp.email.repository.EmailNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailClaimService {

    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(10);

    private final EmailNotificationRepository repository;

    public EmailClaimService(EmailNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<EmailNotification> claim(int limit) {
        List<EmailNotification> notifications = repository.lockEligible(limit);
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(notification -> {
            notification.setStatus(EmailStatus.PROCESSING);
            notification.setProcessingStartedAt(now);
            notification.setNextRetryAt(null);
        });
        return repository.saveAll(notifications);
    }

    @Transactional
    public EmailClaimResult claimById(long notificationId) {
        LocalDateTime now = LocalDateTime.now();
        EmailNotification notification = repository.findByIdForUpdate(notificationId)
                .orElse(null);
        if (notification == null) {
            return EmailClaimResult.deadLetter();
        }

        if (notification.getStatus() == EmailStatus.SENT
                || notification.getStatus() == EmailStatus.CANCELLED) {
            return EmailClaimResult.acknowledge();
        }
        if (notification.getStatus() == EmailStatus.FAILED) {
            return EmailClaimResult.deadLetter();
        }
        if (notification.getStatus() == EmailStatus.PROCESSING
                && !processingLeaseExpired(notification, now)) {
            return EmailClaimResult.retryLater(secondsUntilLeaseExpiry(notification, now));
        }
        if (notification.getStatus() == EmailStatus.RETRY_PENDING
                && notification.getNextRetryAt() != null
                && notification.getNextRetryAt().isAfter(now)) {
            return EmailClaimResult.retryLater(
                    positiveSecondsBetween(now, notification.getNextRetryAt()));
        }
        if (notification.getStatus() != EmailStatus.QUEUED
                && notification.getStatus() != EmailStatus.RETRY_PENDING
                && notification.getStatus() != EmailStatus.PROCESSING) {
            return EmailClaimResult.deadLetter();
        }

        notification.setStatus(EmailStatus.PROCESSING);
        notification.setProcessingStartedAt(now);
        notification.setNextRetryAt(null);
        repository.save(notification);
        return EmailClaimResult.claimed(notification);
    }

    @Transactional
    public void recoverStale() {
        List<EmailNotification> notifications =
                repository.findByStatusAndProcessingStartedAtBefore(
                        EmailStatus.PROCESSING,
                        LocalDateTime.now().minus(PROCESSING_LEASE));
        notifications.forEach(notification -> {
            notification.setStatus(EmailStatus.RETRY_PENDING);
            notification.setNextRetryAt(LocalDateTime.now());
            notification.setFailureReason("Recovered after interrupted processing");
        });
        repository.saveAll(notifications);
    }

    private boolean processingLeaseExpired(
            EmailNotification notification, LocalDateTime now) {
        return notification.getProcessingStartedAt() == null
                || !notification.getProcessingStartedAt()
                        .plus(PROCESSING_LEASE)
                        .isAfter(now);
    }

    private int secondsUntilLeaseExpiry(
            EmailNotification notification, LocalDateTime now) {
        return positiveSecondsBetween(
                now, notification.getProcessingStartedAt().plus(PROCESSING_LEASE));
    }

    private int positiveSecondsBetween(LocalDateTime from, LocalDateTime to) {
        long seconds = Math.max(1, Duration.between(from, to).toSeconds() + 1);
        return (int) Math.min(seconds, Integer.MAX_VALUE);
    }
}
