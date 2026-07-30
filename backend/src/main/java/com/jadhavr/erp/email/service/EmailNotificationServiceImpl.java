package com.jadhavr.erp.email.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.email.config.MailProperties;
import com.jadhavr.erp.email.entity.EmailNotification;
import com.jadhavr.erp.email.enums.EmailPriority;
import com.jadhavr.erp.email.enums.EmailType;
import com.jadhavr.erp.email.repository.EmailNotificationRepository;
import com.jadhavr.erp.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {
    private final EmailNotificationRepository repository;
    private final MailProperties properties;
    private final ObjectMapper objectMapper;
    private final TemplateDataCipher cipher;
    private final String frontendUrl;

    public EmailNotificationServiceImpl(
            EmailNotificationRepository repository,
            MailProperties properties,
            ObjectMapper objectMapper,
            TemplateDataCipher cipher,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cipher = cipher;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional
    public void queueUserCreatedEmail(User user) {
        queueUserCreatedEmail(user, "");
    }

    @Override
    @Transactional
    public void queueUserCreatedEmail(User user, String temporaryPassword) {
        queue(
                user,
                EmailType.USER_ACCOUNT_CREATED,
                "Your Jadhavar ERP account",
                "user-account-created",
                Map.of(
                        "name", user.getFullName(),
                        "username", user.getEmail(),
                        "temporaryPassword", safe(temporaryPassword),
                        "url", frontendUrl + "/login"),
                "USER_CREATED");
    }

    @Override
    @Transactional
    public void queuePrincipalCreatedEmail(User user) {
        queuePrincipalCreatedEmail(user, "");
    }

    @Override
    @Transactional
    public void queuePrincipalCreatedEmail(User user, String temporaryPassword) {
        queue(
                user,
                EmailType.PRINCIPAL_ACCOUNT_CREATED,
                "Principal account created",
                "principal-account-created",
                Map.of(
                        "name", user.getFullName(),
                        "username", user.getEmail(),
                        "temporaryPassword", safe(temporaryPassword),
                        "college", user.getCollege() == null ? "" : user.getCollege().getName(),
                        "url", frontendUrl + "/login"),
                "PRINCIPAL_CREATED");
    }

    @Override
    @Transactional
    public void queueAdmissionCompletedEmail(User user, String admissionReferenceNumber) {
        queue(
                user,
                EmailType.ADMISSION_COMPLETED,
                "Your admission form was submitted successfully",
                "admission-completed",
                Map.of(
                        "name", user.getFullName(),
                        "admissionReferenceNumber", safe(admissionReferenceNumber),
                        "url", frontendUrl + "/student/admission"),
                "ADMISSION_COMPLETED:" + safe(admissionReferenceNumber));
    }

    @Override
    @Transactional
    public void queueAdmissionApprovedEmail(
            User user, String admissionReferenceNumber, String admissionNumber) {
        queue(
                user,
                EmailType.ADMISSION_APPROVED,
                "Your admission was approved successfully",
                "admission-approved",
                Map.of(
                        "name", user.getFullName(),
                        "admissionReferenceNumber", safe(admissionReferenceNumber),
                        "admissionNumber", safe(admissionNumber),
                        "url", frontendUrl + "/student/dashboard"),
                "ADMISSION_APPROVED:" + safe(admissionReferenceNumber));
    }

    @Override
    @Transactional
    public void queueAccountActivatedEmail(User user) {
        queue(user, EmailType.ACCOUNT_ACTIVATED, "Account activated", "account-activated",
                Map.of("name", user.getFullName(), "url", frontendUrl + "/login"), "ACTIVATED");
    }

    @Override
    @Transactional
    public void queueAccountDeactivatedEmail(User user) {
        queue(user, EmailType.ACCOUNT_DEACTIVATED, "Account deactivated", "account-deactivated",
                Map.of("name", user.getFullName()), "DEACTIVATED");
    }

    @Override
    @Transactional
    public void queuePasswordResetEmail(User user, String token, LocalDateTime expiresAt) {
        queue(user, EmailType.PASSWORD_RESET, "Reset your password", "password-reset",
                Map.of(
                        "name", user.getFullName(),
                        "url", frontendUrl + "/reset-password?token=" + token,
                        "expires", expiresAt.toString()),
                "PASSWORD_RESET:" + expiresAt);
    }

    @Override
    @Transactional
    public void queuePasswordChangedEmail(User user) {
        queue(user, EmailType.PASSWORD_CHANGED, "Your password was changed", "password-changed",
                Map.of("name", user.getFullName()),
                "PASSWORD_CHANGED:" + System.currentTimeMillis());
    }

    @Override
    @Transactional
    public void queueEmailVerificationEmail(User user, String token, LocalDateTime expiresAt) {
        queue(user, EmailType.VERIFY_EMAIL, "Verify your email", "verify-email",
                Map.of(
                        "name", user.getFullName(),
                        "url", frontendUrl + "/verify-email?token=" + token,
                        "expires", expiresAt.toString()),
                "VERIFY_EMAIL:" + expiresAt);
    }

    @Override
    @Transactional
    public void queueEmailVerifiedEmail(User user) {
        queue(user, EmailType.EMAIL_VERIFIED, "Email verified", "email-verified",
                Map.of("name", user.getFullName()), "EMAIL_VERIFIED");
    }

    @Override
    @Transactional
    public void queueFeePaymentReminder(
            User user, Long accountId, String admissionNumber, BigDecimal remainingAmount) {
        queue(user, EmailType.FEE_PAYMENT_REMINDER, "Pending fee payment reminder",
                "fee-payment-reminder",
                Map.of(
                        "name", user.getFullName(),
                        "admissionNumber", admissionNumber,
                        "remainingAmount", remainingAmount.toPlainString(),
                        "url", frontendUrl + "/student/fees"),
                "FEE_REMINDER:" + accountId + ":" + LocalDate.now());
    }

    private void queue(
            User user,
            EmailType type,
            String subject,
            String template,
            Map<String, String> data,
            String event) {
        String idempotencyKey =
                event + ":" + user.getId() + ":" + user.getEmail().toLowerCase(Locale.ROOT);
        if (repository.existsByIdempotencyKey(idempotencyKey)) return;
        try {
            EmailNotification notification = new EmailNotification();
            notification.setRecipientEmail(user.getEmail());
            notification.setRecipientName(user.getFullName());
            notification.setEmailType(type);
            notification.setSubject(subject);
            notification.setTemplateName(template);
            notification.setTemplateData(
                    cipher.encrypt(objectMapper.writeValueAsString(data)));
            notification.setMaxRetries(properties.getMaxRetries());
            notification.setCorrelationId(UUID.randomUUID().toString());
            notification.setIdempotencyKey(idempotencyKey);
            notification.setPriority(
                    type == EmailType.PASSWORD_RESET || type == EmailType.VERIFY_EMAIL
                            ? EmailPriority.HIGH
                            : EmailPriority.NORMAL);
            repository.save(notification);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not queue email notification", exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
