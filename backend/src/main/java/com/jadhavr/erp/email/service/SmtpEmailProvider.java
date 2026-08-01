package com.jadhavr.erp.email.service;

import com.jadhavr.erp.email.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SmtpEmailProvider implements EmailProvider {
    private static final String SES_CONFIGURATION_SET_HEADER =
            "X-SES-CONFIGURATION-SET";

    private final JavaMailSender sender;
    private final MailProperties properties;

    public SmtpEmailProvider(JavaMailSender sender, MailProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    @Override
    public EmailProviderResult send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setValidateAddresses(true);
            helper.setFrom(properties.getFromAddress(), properties.getFromName());
            helper.setReplyTo(properties.getReplyTo());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.text(), message.html());
            if (!properties.getConfigurationSet().isBlank()) {
                mimeMessage.setHeader(
                        SES_CONFIGURATION_SET_HEADER,
                        properties.getConfigurationSet());
            }
            sender.send(mimeMessage);
            String messageId = mimeMessage.getMessageID();
            return new EmailProviderResult(
                    messageId == null ? UUID.randomUUID().toString() : messageId);
        } catch (Exception exception) {
            throw new IllegalStateException("SMTP delivery failed", exception);
        }
    }
}
