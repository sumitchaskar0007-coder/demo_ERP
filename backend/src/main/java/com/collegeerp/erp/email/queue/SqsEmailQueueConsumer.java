package com.collegeerp.erp.email.queue;

import com.collegeerp.erp.email.config.MailProperties;
import com.collegeerp.erp.email.worker.EmailClaimResult;
import com.collegeerp.erp.email.worker.EmailClaimService;
import com.collegeerp.erp.email.worker.EmailDeliveryOutcome;
import com.collegeerp.erp.email.worker.EmailWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class SqsEmailQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsEmailQueueConsumer.class);

    private final SqsClient sqsClient;
    private final MailProperties properties;
    private final EmailQueueMessageCodec codec;
    private final EmailClaimService claimService;
    private final EmailWorker worker;
    private final Executor executor;

    public SqsEmailQueueConsumer(
            SqsClient sqsClient,
            MailProperties properties,
            EmailQueueMessageCodec codec,
            EmailClaimService claimService,
            EmailWorker worker,
            Executor executor) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.codec = codec;
        this.claimService = claimService;
        this.worker = worker;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${app.mail.sqs.poll-delay-ms:1000}")
    public void poll() {
        MailProperties.Sqs sqs = properties.getSqs();
        try {
            for (Message message : sqsClient.receiveMessage(
                            ReceiveMessageRequest.builder()
                                    .queueUrl(sqs.getQueueUrl())
                                    .waitTimeSeconds(sqs.getWaitTimeSeconds())
                                    .visibilityTimeout(sqs.getVisibilityTimeoutSeconds())
                                    .maxNumberOfMessages(sqs.getMaxMessages())
                                    .build())
                    .messages()) {
                submit(message);
            }
        } catch (RuntimeException exception) {
            log.error("Email SQS receive failed", exception);
        }
    }

    void handle(Message message) {
        long notificationId;
        try {
            notificationId = codec.decode(message.body());
        } catch (IllegalArgumentException exception) {
            // Do not log the untrusted message body. Repeated invalid messages move to the DLQ.
            log.warn("Rejected invalid email SQS message id={}", message.messageId());
            return;
        }

        EmailClaimResult claim;
        try {
            claim = claimService.claimById(notificationId);
        } catch (RuntimeException exception) {
            log.error("Could not claim email notification id={}", notificationId, exception);
            return;
        }

        switch (claim.action()) {
            case ACKNOWLEDGE -> delete(message, notificationId);
            case RETRY_LATER ->
                    defer(message, notificationId, claim.retryAfterSeconds());
            case DEAD_LETTER -> {
                // Leave the message unacknowledged so the queue redrive policy handles it.
            }
            case CLAIMED -> deliver(message, notificationId, claim);
        }
    }

    private void submit(Message message) {
        try {
            executor.execute(() -> handle(message));
        } catch (RejectedExecutionException exception) {
            // Backpressure is handled by visibility timeout/redelivery.
            log.warn("Email worker executor is saturated; leaving message id={} for redelivery",
                    message.messageId());
        }
    }

    private void deliver(
            Message message, long notificationId, EmailClaimResult claim) {
        try {
            EmailDeliveryOutcome outcome = worker.process(claim.notification());
            if (outcome.wasSent()) {
                delete(message, notificationId);
            } else if (outcome.shouldRetry()) {
                defer(message, notificationId, outcome.retryAfterSeconds());
            }
            // FAILED remains unacknowledged so SQS moves the ID-only message to the DLQ.
        } catch (RuntimeException exception) {
            log.error("Unexpected email delivery failure id={}", notificationId, exception);
        }
    }

    private void delete(Message message, long notificationId) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(properties.getSqs().getQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (RuntimeException exception) {
            // SENT/CANCELLED rows make a later redelivery safe to acknowledge again.
            log.error("Could not acknowledge email notification id={}",
                    notificationId, exception);
        }
    }

    private void defer(
            Message message, long notificationId, int retryAfterSeconds) {
        int visibilityTimeout = Math.max(1, Math.min(retryAfterSeconds, 43_200));
        try {
            sqsClient.changeMessageVisibility(
                    ChangeMessageVisibilityRequest.builder()
                            .queueUrl(properties.getSqs().getQueueUrl())
                            .receiptHandle(message.receiptHandle())
                            .visibilityTimeout(visibilityTimeout)
                            .build());
        } catch (RuntimeException exception) {
            log.error("Could not defer email notification id={}", notificationId, exception);
        }
    }
}
