package com.collegeerp.erp.email.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.email.queue.EmailQueueMessageCodec;
import com.collegeerp.erp.email.queue.EmailQueuePublisher;
import com.collegeerp.erp.email.queue.SqsEmailPublicationRecovery;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

class EmailSqsConfigurationTest {

    @Test
    void rejectsMissingQueueUrlRegionAndRoles() {
        MailProperties properties = new MailProperties();
        assertThrows(
                IllegalStateException.class,
                () -> EmailSqsConfiguration.validate(properties, ""));

        properties.getSqs().setPublisherEnabled(true);
        assertThrows(
                IllegalStateException.class,
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));

        properties.getSqs().setQueueUrl(
                "https://sqs.ap-south-1.amazonaws.com/123456789012/email");
        assertDoesNotThrow(
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));
    }

    @Test
    void consumerRequiresDeliveryToBeEnabled() {
        MailProperties properties = new MailProperties();
        properties.getSqs().setConsumerEnabled(true);
        properties.getSqs().setQueueUrl(
                "https://sqs.ap-south-1.amazonaws.com/123456789012/email");

        assertThrows(
                IllegalStateException.class,
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));

        properties.setEnabled(true);
        assertDoesNotThrow(
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));
    }

    @Test
    void rejectsSqsRoleFlagsWhenDatabaseTransportIsStillSelected() {
        MailProperties properties = new MailProperties();
        properties.getSqs().setPublisherEnabled(true);

        assertThrows(
                IllegalStateException.class,
                properties::validateTransportSelection);

        properties.setTransport(MailProperties.Transport.SQS);
        assertDoesNotThrow(properties::validateTransportSelection);
    }

    @Test
    void recoveryRequiresPublisherRole() {
        MailProperties properties = new MailProperties();
        properties.setTransport(MailProperties.Transport.SQS);
        properties.getSqs().setConsumerEnabled(true);
        properties.getSqs().setRecoveryEnabled(true);
        properties.setEnabled(true);
        properties.getSqs().setQueueUrl(
                "https://sqs.ap-south-1.amazonaws.com/123456789012/email");

        assertThrows(
                IllegalStateException.class,
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));

        properties.getSqs().setPublisherEnabled(true);
        assertDoesNotThrow(
                () -> EmailSqsConfiguration.validate(properties, "ap-south-1"));
    }

    @Test
    void publisherRoleDoesNotAccidentallyEnableRecoveryScheduler() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class, EmailSqsConfiguration.class)
                .withPropertyValues(
                        "app.mail.transport=sqs",
                        "app.mail.sqs.publisher-enabled=true",
                        "app.mail.sqs.consumer-enabled=false",
                        "app.mail.sqs.recovery-enabled=false",
                        "app.mail.sqs.queue-url=https://sqs.ap-south-1.amazonaws.com/123456789012/email",
                        "app.aws.region=ap-south-1")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailQueuePublisher.class);
                    assertThat(context).doesNotHaveBean(SqsEmailPublicationRecovery.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailProperties.class)
    static class TestConfiguration {
        @Bean
        EmailQueueMessageCodec emailQueueMessageCodec() {
            return new EmailQueueMessageCodec(new ObjectMapper());
        }
    }
}
