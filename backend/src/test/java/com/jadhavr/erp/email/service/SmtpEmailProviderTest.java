package com.jadhavr.erp.email.service;

import com.jadhavr.erp.email.config.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailProviderTest {
    @Test
    void sendsMultipartMessageWithSesConfigurationSet() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(mime);
        MailProperties properties = new MailProperties();
        properties.setFromAddress("noreply@jadhavaredu.com");
        properties.setReplyTo("noreply@jadhavaredu.com");
        properties.setFromName("Jadhavar ERP");
        properties.setConfigurationSet("jadhavr-erp-production");

        SmtpEmailProvider provider = new SmtpEmailProvider(sender, properties);
        provider.send(new EmailMessage(
                "student@example.com", "Student", "Account ready", "<p>Ready</p>", "Ready"));

        verify(sender).send(mime);
        assertEquals("jadhavr-erp-production",
                mime.getHeader("X-SES-CONFIGURATION-SET", null));
        assertEquals("Account ready", mime.getSubject());
    }
}
