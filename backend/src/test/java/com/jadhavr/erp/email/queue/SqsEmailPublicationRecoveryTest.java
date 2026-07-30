package com.jadhavr.erp.email.queue;

import com.jadhavr.erp.email.config.MailProperties;
import com.jadhavr.erp.email.enums.EmailStatus;
import com.jadhavr.erp.email.repository.EmailNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsEmailPublicationRecoveryTest {

    @Mock private EmailNotificationRepository repository;
    @Mock private EmailQueuePublisher publisher;

    @Test
    void advancesThroughStrandedRowsAndWrapsTheScanCursor() {
        MailProperties properties = new MailProperties();
        properties.getSqs().setRecoveryBatchSize(2);
        when(repository.findIdsByStatusCreatedBeforeAndIdAfter(
                        eq(EmailStatus.QUEUED),
                        any(LocalDateTime.class),
                        eq(0L),
                        any(Pageable.class)))
                .thenReturn(List.of(11L, 12L));
        when(repository.findIdsByStatusCreatedBeforeAndIdAfter(
                        eq(EmailStatus.QUEUED),
                        any(LocalDateTime.class),
                        eq(12L),
                        any(Pageable.class)))
                .thenReturn(List.of());
        SqsEmailPublicationRecovery recovery =
                new SqsEmailPublicationRecovery(repository, properties, publisher);

        recovery.republishStranded();
        recovery.republishStranded();

        verify(publisher, times(2)).publish(11L);
        verify(publisher, times(2)).publish(12L);
    }
}
