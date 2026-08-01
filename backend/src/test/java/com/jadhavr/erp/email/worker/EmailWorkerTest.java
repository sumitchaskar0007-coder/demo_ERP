package com.jadhavr.erp.email.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.email.config.MailProperties;
import com.jadhavr.erp.email.entity.EmailNotification;
import com.jadhavr.erp.email.enums.EmailStatus;
import com.jadhavr.erp.email.repository.EmailNotificationRepository;
import com.jadhavr.erp.email.service.EmailMessage;
import com.jadhavr.erp.email.service.EmailProvider;
import com.jadhavr.erp.email.service.EmailProviderResult;
import com.jadhavr.erp.email.service.TemplateDataCipher;
import com.jadhavr.erp.email.template.EmailTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailWorkerTest {

    @Mock private EmailNotificationRepository repository;
    @Mock private EmailProvider provider;
    @Mock private EmailClaimService claims;
    @Mock private EmailTemplateRenderer renderer;

    private MailProperties properties;
    private TemplateDataCipher cipher;
    private EmailWorker worker;

    @BeforeEach
    void setUp() {
        properties = new MailProperties();
        properties.setRetryDelaySeconds(60);
        cipher = new TemplateDataCipher("email-worker-test-encryption-secret");
        worker = new EmailWorker(
                repository,
                provider,
                properties,
                new ObjectMapper(),
                cipher,
                Runnable::run,
                claims,
                renderer);
        when(renderer.render(any(), any(), any())).thenReturn("<p>message</p>");
    }

    @Test
    void providerFailureUsesExponentialRetryState() {
        EmailNotification notification = notification(0, 3);
        when(provider.send(any(EmailMessage.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));

        EmailDeliveryOutcome outcome = worker.process(notification);

        assertEquals(EmailStatus.RETRY_PENDING, outcome.status());
        assertTrue(outcome.retryAfterSeconds() >= 60);
        assertTrue(outcome.retryAfterSeconds() <= 74);
        assertEquals(1, notification.getRetryCount());
        assertTrue(notification.getNextRetryAt() != null);
        verify(repository).save(notification);
    }

    @Test
    void exhaustedRetryBecomesFailedForQueueRedrive() {
        EmailNotification notification = notification(2, 3);
        when(provider.send(any(EmailMessage.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));

        EmailDeliveryOutcome outcome = worker.process(notification);

        assertEquals(EmailStatus.FAILED, outcome.status());
        assertEquals(3, notification.getRetryCount());
        assertTrue(notification.getNextRetryAt() == null);
        verify(repository).save(notification);
    }

    @Test
    void successfulDeliveryRedactsSensitiveTemplateData() {
        EmailNotification notification = notification(0, 3);
        notification.setTemplateData(cipher.encrypt(
                "{\"name\":\"Student\",\"temporaryPassword\":\"Secret@123\"}"));
        when(provider.send(any(EmailMessage.class)))
                .thenReturn(new EmailProviderResult("ses-message-id"));

        EmailDeliveryOutcome outcome = worker.process(notification);

        assertEquals(EmailStatus.SENT, outcome.status());
        assertEquals("{}", cipher.decrypt(notification.getTemplateData()));
        assertEquals("ses-message-id", notification.getProviderMessageId());
        verify(repository).save(notification);
    }

    private EmailNotification notification(int retryCount, int maxRetries) {
        EmailNotification notification = new EmailNotification();
        notification.setRecipientEmail("student@example.com");
        notification.setRecipientName("Student");
        notification.setSubject("Subject");
        notification.setTemplateName("template");
        notification.setTemplateData(cipher.encrypt("{\"name\":\"Student\"}"));
        notification.setRetryCount(retryCount);
        notification.setMaxRetries(maxRetries);
        notification.setStatus(EmailStatus.PROCESSING);
        return notification;
    }
}
