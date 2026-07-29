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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

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
        if (!properties.isEnabled()) return;
        claims.recoverStale();
        for (EmailNotification notification : claims.claim(properties.getBatchSize())) {
            executor.execute(() -> process(notification));
        }
    }

    @Transactional
    protected List<EmailNotification> claim() {
        List<EmailNotification> notifications =
                repository.lockEligible(properties.getBatchSize());
        notifications.forEach(notification -> {
            notification.setStatus(EmailStatus.PROCESSING);
            notification.setProcessingStartedAt(LocalDateTime.now());
        });
        return repository.saveAll(notifications);
    }

    protected void process(EmailNotification notification) {
        try {
            Map<String, String> data = objectMapper.readValue(
                    cipher.decrypt(notification.getTemplateData()),
                    new TypeReference<>() {});
            String name = data.getOrDefault("name", "User");
            String url = data.get("url");
            String text = "Hello " + name + ",\n\n" + notification.getSubject()
                    + credentialText(data)
                    + admissionText(data)
                    + (url == null ? "" : "\n\n" + url)
                    + "\n\nJadhavar ERP";
            String html = renderer.render(
                    notification.getTemplateName(), data, notification.getSubject());
            EmailProviderResult result = provider.send(new EmailMessage(
                    notification.getRecipientEmail(),
                    name,
                    notification.getSubject(),
                    html,
                    text));
            sent(notification, result);
        } catch (Exception exception) {
            failed(notification, exception);
        }
    }

    @Transactional
    protected void sent(EmailNotification notification, EmailProviderResult result) {
        notification.setStatus(EmailStatus.SENT);
        notification.setProvider("SMTP");
        notification.setProviderMessageId(result.providerMessageId());
        notification.setSentAt(LocalDateTime.now());
        notification.setFailureReason(null);
        repository.save(notification);
    }

    @Transactional
    protected void failed(EmailNotification notification, Exception exception) {
        int count = notification.getRetryCount() + 1;
        notification.setRetryCount(count);
        notification.setFailureReason("Email delivery failed");
        if (count >= notification.getMaxRetries()) {
            notification.setStatus(EmailStatus.FAILED);
        } else {
            notification.setStatus(EmailStatus.RETRY_PENDING);
            long delay = (long) properties.getRetryDelaySeconds()
                    * (1L << Math.min(count - 1, 4))
                    + new Random().nextInt(15);
            notification.setNextRetryAt(LocalDateTime.now().plusSeconds(delay));
        }
        repository.save(notification);
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
}
