package com.jadhavr.erp.email.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MailPropertiesTest {
    @Test
    void acceptsProductionSenderAndConfigurationSet() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setFromAddress("noreply@jadhavaredu.com");
        properties.setReplyTo("noreply@jadhavaredu.com");
        properties.setConfigurationSet("jadhavr-erp-production");

        assertDoesNotThrow(properties::validateTransportSelection);
    }

    @Test
    void rejectsInvalidSenderAndConfigurationSet() {
        MailProperties invalidSender = new MailProperties();
        invalidSender.setEnabled(true);
        invalidSender.setFromAddress("not-an-email");
        invalidSender.setReplyTo("noreply@jadhavaredu.com");
        assertThrows(IllegalStateException.class, invalidSender::validateTransportSelection);

        MailProperties invalidSet = new MailProperties();
        invalidSet.setConfigurationSet("unsafe header\nvalue");
        assertThrows(IllegalStateException.class, invalidSet::validateTransportSelection);
    }
}
