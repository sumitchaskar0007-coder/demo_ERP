package com.jadhavr.erp.email.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EmailQueueDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailQueueDispatcher.class);

    private final ObjectProvider<EmailQueuePublisher> publisherProvider;

    public EmailQueueDispatcher(ObjectProvider<EmailQueuePublisher> publisherProvider) {
        this.publisherProvider = publisherProvider;
    }

    public void publishAfterCommit(long notificationId) {
        requireValidId(notificationId);
        if (publisherProvider.getIfAvailable() == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishSafely(notificationId);
                        }
                    });
            return;
        }
        publishSafely(notificationId);
    }

    public void publishNow(long notificationId) {
        requireValidId(notificationId);
        EmailQueuePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.publish(notificationId);
        }
    }

    private void publishSafely(long notificationId) {
        try {
            publishNow(notificationId);
        } catch (RuntimeException exception) {
            // The database row is the durable outbox. Recovery republishes stranded QUEUED rows.
            log.error("Could not publish queued email notification id={}", notificationId, exception);
        }
    }

    private void requireValidId(long notificationId) {
        if (notificationId < 1) {
            throw new IllegalArgumentException("Email notification ID must be positive");
        }
    }
}
