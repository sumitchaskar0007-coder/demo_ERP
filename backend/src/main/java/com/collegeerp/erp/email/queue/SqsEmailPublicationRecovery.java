package com.collegeerp.erp.email.queue;

import com.collegeerp.erp.email.config.MailProperties;
import com.collegeerp.erp.email.enums.EmailStatus;
import com.collegeerp.erp.email.repository.EmailNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SqsEmailPublicationRecovery {

    private static final Logger log =
            LoggerFactory.getLogger(SqsEmailPublicationRecovery.class);

    private final EmailNotificationRepository repository;
    private final MailProperties properties;
    private final EmailQueuePublisher publisher;
    private final AtomicLong scanCursor = new AtomicLong();

    public SqsEmailPublicationRecovery(
            EmailNotificationRepository repository,
            MailProperties properties,
            EmailQueuePublisher publisher) {
        this.repository = repository;
        this.properties = properties;
        this.publisher = publisher;
    }

    @Scheduled(
            initialDelayString = "${app.mail.sqs.recovery-delay-ms:60000}",
            fixedDelayString = "${app.mail.sqs.recovery-delay-ms:60000}")
    public void republishStranded() {
        MailProperties.Sqs sqs = properties.getSqs();
        LocalDateTime cutoff =
                LocalDateTime.now().minusSeconds(sqs.getRecoveryAgeSeconds());
        List<Long> notificationIds = strandedAfter(cutoff, scanCursor.get());
        if (notificationIds.isEmpty() && scanCursor.get() > 0) {
            scanCursor.set(0);
            notificationIds = strandedAfter(cutoff, 0);
        }
        for (Long notificationId : notificationIds) {
            try {
                publisher.publish(notificationId);
            } catch (RuntimeException exception) {
                log.error("Could not recover email publication id={}",
                        notificationId, exception);
            }
            scanCursor.set(notificationId);
        }
    }

    private List<Long> strandedAfter(LocalDateTime cutoff, long afterId) {
        return repository.findIdsByStatusCreatedBeforeAndIdAfter(
                EmailStatus.QUEUED,
                cutoff,
                afterId,
                PageRequest.of(0, properties.getSqs().getRecoveryBatchSize()));
    }
}
