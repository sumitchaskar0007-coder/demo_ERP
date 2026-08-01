package com.jadhavr.erp.email.worker;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EmailWorker {

    private final EmailNotificationRepository repository;
    private final EmailProvider provider;
    private final MailProperties properties;
    private final ObjectMapper objectMapper;
    private final TemplateDataCipher cipher;
    private final Executor executor;
    private final EmailClaimService claims;
    private final EmailTemplateRenderer renderer;

    public EmailWorker(
            EmailNotificationRepository repository,
            EmailProvider provider,
            MailProperties properties,
            ObjectMapper objectMapper,
            TemplateDataCipher cipher,
            @Qualifier("emailTaskExecutor") Executor executor,
            EmailClaimService claims,
            EmailTemplateRenderer renderer) {
        this.repository = repository;
        this.provider = provider;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cipher = cipher;
        this.executor = executor;
        this.claims = claims;
        this.renderer = renderer;
    }

    @Scheduled(fixedDelayString = "${app.mail.poll-delay-ms:10000}")
    public void poll() {
        if (!properties.isEnabled() || !properties.isDatabaseTransport()) {
            return;
        }
        claims.recoverStale();
        for (EmailNotification notification : claims.claim(properties.getBatchSize())) {
            executor.execute(() -> process(notification));
        }
    }

    public EmailDeliveryOutcome process(EmailNotification notification) {
        EmailProviderResult result;
        String redactedTemplateData;
        try {
            Map<String, String> data = objectMapper.readValue(
                    cipher.decrypt(notification.getTemplateData()),
                    new TypeReference<>() {});
            // Prepare the redacted value before delivery. This prevents a successful
            // provider send from being followed by a cipher failure and a duplicate retry.
            redactedTemplateData = cipher.encrypt("{}");
            String name = data.getOrDefault("name", "User");
            String url = data.get("url");
            String text = "Hello " + name + ",\n\n" + notification.getSubject()
                    + credentialText(data)
                    + admissionText(data)
                    + (url == null ? "" : "\n\n" + url)
                    + "\n\nJadhavar Institute";
            String html = renderer.render(
                    notification.getTemplateName(), data, notification.getSubject());
            result = provider.send(new EmailMessage(
                    notification.getRecipientEmail(),
                    name,
                    notification.getSubject(),
                    html,
                    text));
        } catch (Exception exception) {
            return markFailed(notification);
        }
        // Persist outside the delivery catch. A database failure must leave the queue
        // message unacknowledged instead of pretending that the provider send failed.
        markSent(notification, result, redactedTemplateData);
        return new EmailDeliveryOutcome(EmailStatus.SENT, 0);
    }

    protected void markSent(
            EmailNotification notification,
            EmailProviderResult result,
            String redactedTemplateData) {
        notification.setStatus(EmailStatus.SENT);
        notification.setProvider("SMTP");
        notification.setProviderMessageId(result.providerMessageId());
        notification.setSentAt(LocalDateTime.now());
        notification.setFailureReason(null);
        notification.setProcessingStartedAt(null);
        notification.setNextRetryAt(null);
        // Credentials and password-reset URLs are needed only until delivery succeeds.
        notification.setTemplateData(redactedTemplateData);
        repository.save(notification);
    }

    protected EmailDeliveryOutcome markFailed(EmailNotification notification) {
        int retryCount = notification.getRetryCount() + 1;
        notification.setRetryCount(retryCount);
        notification.setFailureReason("Email delivery failed");
        notification.setProcessingStartedAt(null);
        if (retryCount >= notification.getMaxRetries()) {
            notification.setStatus(EmailStatus.FAILED);
            notification.setNextRetryAt(null);
            repository.save(notification);
            return new EmailDeliveryOutcome(EmailStatus.FAILED, 0);
        }

        int retryAfterSeconds = retryDelaySeconds(retryCount);
        notification.setStatus(EmailStatus.RETRY_PENDING);
        notification.setNextRetryAt(LocalDateTime.now().plusSeconds(retryAfterSeconds));
        repository.save(notification);
        return new EmailDeliveryOutcome(
                EmailStatus.RETRY_PENDING, retryAfterSeconds);
    }

    private String credentialText(Map<String, String> data) {
        String username = data.get("username");
        String temporaryPassword = data.get("temporaryPassword");
        if (username == null || temporaryPassword == null || temporaryPassword.isBlank()) {
            return "";
        }
        return "\n\nUsername: " + username
                + "\nTemporary password: " + temporaryPassword
                + "\nPlease sign in and change this temporary password.";
    }

    private String admissionText(Map<String, String> data) {
        String reference = data.get("admissionReferenceNumber");
        String admissionNumber = data.get("admissionNumber");
        StringBuilder result = new StringBuilder();
        if (reference != null && !reference.isBlank()) {
            result.append("\n\nAdmission reference: ").append(reference);
        }
        if (admissionNumber != null && !admissionNumber.isBlank()) {
            result.append("\nAdmission number: ").append(admissionNumber);
        }
        return result.toString();
    }

    private int retryDelaySeconds(int retryCount) {
        long exponentialDelay = (long) properties.getRetryDelaySeconds()
                * (1L << Math.min(retryCount - 1, 4));
        long withJitter = exponentialDelay + ThreadLocalRandom.current().nextInt(15);
        return (int) Math.min(withJitter, 43_200);
    }
}
