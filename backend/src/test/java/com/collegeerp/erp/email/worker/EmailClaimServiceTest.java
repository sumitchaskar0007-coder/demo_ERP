package com.collegeerp.erp.email.worker;

import com.collegeerp.erp.email.entity.EmailNotification;
import com.collegeerp.erp.email.enums.EmailStatus;
import com.collegeerp.erp.email.repository.EmailNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailClaimServiceTest {

    @Mock private EmailNotificationRepository repository;

    @Test
    void claimsQueuedRowWithProcessingLease() {
        EmailNotification notification = notification(EmailStatus.QUEUED);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(notification));
        EmailClaimService service = new EmailClaimService(repository);

        EmailClaimResult result = service.claimById(42L);

        assertEquals(EmailClaimResult.Action.CLAIMED, result.action());
        assertEquals(EmailStatus.PROCESSING, notification.getStatus());
        assertTrue(notification.getProcessingStartedAt() != null);
        verify(repository).save(notification);
    }

    @Test
    void doesNotClaimAlreadySentOrActivelyProcessingRow() {
        EmailNotification sent = notification(EmailStatus.SENT);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(sent));
        EmailClaimService service = new EmailClaimService(repository);

        assertEquals(
                EmailClaimResult.Action.ACKNOWLEDGE,
                service.claimById(42L).action());
        verify(repository, never()).save(sent);

        EmailNotification processing = notification(EmailStatus.PROCESSING);
        processing.setProcessingStartedAt(LocalDateTime.now());
        when(repository.findByIdForUpdate(43L)).thenReturn(Optional.of(processing));

        EmailClaimResult active = service.claimById(43L);

        assertEquals(EmailClaimResult.Action.RETRY_LATER, active.action());
        assertTrue(active.retryAfterSeconds() > 0);
        verify(repository, never()).save(processing);
    }

    private EmailNotification notification(EmailStatus status) {
        EmailNotification notification = new EmailNotification();
        notification.setStatus(status);
        return notification;
    }
}
