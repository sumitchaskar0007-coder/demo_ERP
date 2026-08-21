package com.collegeerp.erp.email.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailPropertiesTest {
    @Test
    void acceptsProductionSenderAndConfigurationSet() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setFromAddress("noreply@collegeerp.example");
        properties.setReplyTo("noreply@collegeerp.example");
        properties.setConfigurationSet("college-erp-production");

        assertDoesNotThrow(properties::validateTransportSelection);
    }

    @Test
    void rejectsInvalidSenderAndConfigurationSet() {
        MailProperties invalidSender = new MailProperties();
        invalidSender.setEnabled(true);
        invalidSender.setFromAddress("not-an-email");
        invalidSender.setReplyTo("noreply@collegeerp.example");
        assertThrows(IllegalStateException.class, invalidSender::validateTransportSelection);

        MailProperties invalidSet = new MailProperties();
        invalidSet.setConfigurationSet("unsafe header\nvalue");
        assertThrows(IllegalStateException.class, invalidSet::validateTransportSelection);
    }
}
