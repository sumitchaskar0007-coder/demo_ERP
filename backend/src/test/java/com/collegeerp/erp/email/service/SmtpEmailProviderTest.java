package com.collegeerp.erp.email.service;

import com.collegeerp.erp.email.config.MailProperties;
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
        properties.setFromAddress("noreply@collegeerp.example");
        properties.setReplyTo("noreply@collegeerp.example");
        properties.setFromName("College ERP");
        properties.setConfigurationSet("college-erp-production");

        SmtpEmailProvider provider = new SmtpEmailProvider(sender, properties);
        provider.send(new EmailMessage(
                "student@example.com", "Student", "Account ready", "<p>Ready</p>", "Ready"));

        verify(sender).send(mime);
        assertEquals("college-erp-production",
                mime.getHeader("X-SES-CONFIGURATION-SET", null));
        assertEquals("Account ready", mime.getSubject());
    }
}
