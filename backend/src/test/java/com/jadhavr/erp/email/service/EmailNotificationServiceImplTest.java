package com.jadhavr.erp.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.email.config.MailProperties;
import com.jadhavr.erp.email.entity.EmailNotification;
import com.jadhavr.erp.email.enums.EmailType;
import com.jadhavr.erp.email.repository.EmailNotificationRepository;
import com.jadhavr.erp.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceImplTest {
    @Mock private EmailNotificationRepository repository;
    private AutoCloseable mocks;
    private EmailNotificationServiceImpl service;
    private TemplateDataCipher cipher;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        cipher = new TemplateDataCipher("test-secret-for-email-encryption");
        service = new EmailNotificationServiceImpl(
                repository,
                new MailProperties(),
                new ObjectMapper(),
                cipher,
                "http://localhost:5173");
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void queuesEncryptedTemporaryCredentials() throws Exception {
        User user = user();

        service.queueUserCreatedEmail(user, "Temp@123");

        ArgumentCaptor<EmailNotification> captor =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(captor.capture());
        EmailNotification notification = captor.getValue();
        Assertions.assertFalse(notification.getTemplateData().contains("Temp@123"));
        Map<String, String> data = new ObjectMapper().readValue(
                cipher.decrypt(notification.getTemplateData()), Map.class);
        Assertions.assertEquals("test@example.com", data.get("username"));
        Assertions.assertEquals("Temp@123", data.get("temporaryPassword"));
    }

    @Test
    void queuesAdmissionApprovalNotification() {
        service.queueAdmissionApprovedEmail(user(), "ADM-REF", "ADM-NO");

        ArgumentCaptor<EmailNotification> captor =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(repository).save(captor.capture());
        Assertions.assertEquals(EmailType.ADMISSION_APPROVED, captor.getValue().getEmailType());
        Assertions.assertEquals(
                "ADMISSION_APPROVED:ADM-REF:42:test@example.com",
                captor.getValue().getIdempotencyKey());
    }

    @Test
    void duplicateEventIsNotQueued() {
        when(repository.existsByIdempotencyKey("USER_CREATED:42:test@example.com"))
                .thenReturn(true);
        service.queueUserCreatedEmail(user());
        verify(repository, never()).save(any());
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        return user;
    }
}
