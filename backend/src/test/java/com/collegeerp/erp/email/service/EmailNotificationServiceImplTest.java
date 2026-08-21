package com.collegeerp.erp.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.email.config.MailProperties;
import com.collegeerp.erp.email.entity.EmailNotification;
import com.collegeerp.erp.email.queue.EmailQueueDispatcher;
import com.collegeerp.erp.email.repository.EmailNotificationRepository;
import com.collegeerp.erp.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock private EmailNotificationRepository repository;
    @Mock private EmailQueueDispatcher queueDispatcher;

    private EmailNotificationServiceImpl service;
    private TemplateDataCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = new TemplateDataCipher("test-secret-for-email-encryption");
        service = new EmailNotificationServiceImpl(
                repository,
                new MailProperties(),
                new ObjectMapper(),
                cipher,
                queueDispatcher,
                "http://localhost:5173");
    }

    @Test
    @SuppressWarnings("unchecked")
    void accountCreatedNotificationContainsEncryptedTemporaryCredentials() throws Exception {
        when(repository.save(any(EmailNotification.class))).thenAnswer(invocation -> {
            EmailNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 74L);
            return notification;
        });

        service.queueUserCreatedEmail(user(), "Temp@123");

        ArgumentCaptor<EmailNotification> notification =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(notification.capture());
        Map<String, String> data = new ObjectMapper().readValue(
                cipher.decrypt(notification.getValue().getTemplateData()), Map.class);
        assertEquals("test@example.com", data.get("username"));
        assertEquals("Temp@123", data.get("temporaryPassword"));
        assertEquals("http://localhost:5173/login", data.get("url"));
    }

    @Test
    void queuesAdmissionCompletionOncePerReference() {
        when(repository.save(any(EmailNotification.class))).thenAnswer(invocation -> {
            EmailNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 75L);
            return notification;
        });

        service.queueAdmissionCompletedEmail(user(), "ADM-2026-001");

        ArgumentCaptor<EmailNotification> notification =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(notification.capture());
        assertEquals(
                "ADMISSION_COMPLETED:ADM-2026-001:42:test@example.com",
                notification.getValue().getIdempotencyKey());
        verify(queueDispatcher).publishAfterCommit(75L);
    }

    @Test
    void queuesEncryptedIdempotentUserCreatedNotificationAndPublishesOnlyItsId() {
        when(repository.save(any(EmailNotification.class))).thenAnswer(invocation -> {
            EmailNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 73L);
            return notification;
        });
        User user = user();

        service.queueUserCreatedEmail(user);

        ArgumentCaptor<EmailNotification> notification =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(notification.capture());
        assertFalse(notification.getValue().getTemplateData().contains("Test User"));
        assertEquals(
                "USER_CREATED:42:test@example.com",
                notification.getValue().getIdempotencyKey());
        verify(queueDispatcher).publishAfterCommit(73L);
    }

    @Test
    void duplicateEventIsNotQueuedOrPublished() {
        when(repository.existsByIdempotencyKey(
                        "USER_CREATED:42:test@example.com"))
                .thenReturn(true);

        service.queueUserCreatedEmail(user());

        verify(repository, never()).save(any());
        verify(queueDispatcher, never()).publishAfterCommit(anyLong());
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        return user;
    }
}
