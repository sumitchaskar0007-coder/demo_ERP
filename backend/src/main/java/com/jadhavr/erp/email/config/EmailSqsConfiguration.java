package com.jadhavr.erp.email.config;

import com.jadhavr.erp.email.queue.EmailQueueMessageCodec;
import com.jadhavr.erp.email.queue.EmailQueuePublisher;
import com.jadhavr.erp.email.queue.SqsEmailPublicationRecovery;
import com.jadhavr.erp.email.queue.SqsEmailQueueConsumer;
import com.jadhavr.erp.email.queue.SqsEmailQueuePublisher;
import com.jadhavr.erp.email.repository.EmailNotificationRepository;
import com.jadhavr.erp.email.worker.EmailClaimService;
import com.jadhavr.erp.email.worker.EmailWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.mail.transport", havingValue = "sqs")
public class EmailSqsConfiguration {

    @Bean(name = "emailSqsClient", destroyMethod = "close")
    SqsClient emailSqsClient(
            MailProperties properties,
            @Value("${app.aws.region:}") String awsRegion) {
        validate(properties, awsRegion);
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.mail.sqs.publisher-enabled",
            havingValue = "true")
    EmailQueuePublisher emailQueuePublisher(
            @Qualifier("emailSqsClient") SqsClient sqsClient,
            MailProperties properties,
            EmailQueueMessageCodec codec) {
        return new SqsEmailQueuePublisher(sqsClient, properties, codec);
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.mail.sqs.recovery-enabled",
            havingValue = "true")
    SqsEmailPublicationRecovery sqsEmailPublicationRecovery(
            EmailNotificationRepository repository,
            MailProperties properties,
            EmailQueuePublisher publisher) {
        return new SqsEmailPublicationRecovery(repository, properties, publisher);
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.mail.sqs.consumer-enabled",
            havingValue = "true")
    SqsEmailQueueConsumer sqsEmailQueueConsumer(
            @Qualifier("emailSqsClient") SqsClient sqsClient,
            MailProperties properties,
            EmailQueueMessageCodec codec,
            EmailClaimService claimService,
            EmailWorker worker,
            @Qualifier("emailTaskExecutor") Executor executor) {
        return new SqsEmailQueueConsumer(
                sqsClient, properties, codec, claimService, worker, executor);
    }

    static void validate(MailProperties properties, String awsRegion) {
        MailProperties.Sqs sqs = properties.getSqs();
        if (!sqs.isPublisherEnabled() && !sqs.isConsumerEnabled()) {
            throw new IllegalStateException(
                    "SQS mail transport requires a publisher or consumer role");
        }
        if (sqs.isRecoveryEnabled() && !sqs.isPublisherEnabled()) {
            throw new IllegalStateException(
                    "Email SQS recovery requires the publisher role");
        }
        if (awsRegion == null || awsRegion.isBlank()) {
            throw new IllegalStateException(
                    "SQS mail transport requires app.aws.region");
        }
        if (sqs.getQueueUrl() == null || sqs.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "SQS mail transport requires app.mail.sqs.queue-url");
        }
        validateQueueUrl(sqs.getQueueUrl());
        if (sqs.isConsumerEnabled() && !properties.isEnabled()) {
            throw new IllegalStateException(
                    "SQS email consumer requires app.mail.enabled=true");
        }
        if (sqs.getWaitTimeSeconds() < 1 || sqs.getWaitTimeSeconds() > 20) {
            throw new IllegalStateException(
                    "Email SQS wait time must be between 1 and 20 seconds");
        }
        if (sqs.getVisibilityTimeoutSeconds() < 1
                || sqs.getVisibilityTimeoutSeconds() > 43_200) {
            throw new IllegalStateException(
                    "Email SQS visibility timeout must be between 1 and 43200 seconds");
        }
        if (sqs.getMaxMessages() < 1 || sqs.getMaxMessages() > 10) {
            throw new IllegalStateException(
                    "Email SQS max messages must be between 1 and 10");
        }
        if (sqs.getPollDelayMs() < 250
                || sqs.getRecoveryDelayMs() < 1_000
                || sqs.getRecoveryAgeSeconds() < 1
                || sqs.getRecoveryBatchSize() < 1
                || sqs.getRecoveryBatchSize() > 1_000) {
            throw new IllegalStateException("Invalid email SQS recovery or polling limits");
        }
    }

    private static void validateQueueUrl(String queueUrl) {
        try {
            URI uri = URI.create(queueUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getPath() == null
                    || uri.getPath().isBlank()) {
                throw new IllegalStateException(
                        "app.mail.sqs.queue-url must be an HTTPS queue URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "app.mail.sqs.queue-url must be a valid queue URL", exception);
        }
    }
}
