package com.jadhavr.erp.reports.transport;

import com.jadhavr.erp.reports.config.ReportExportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ReportJobQueue {
    private static final Logger log = LoggerFactory.getLogger(ReportJobQueue.class);

    private final ReportExportProperties properties;
    private final ObjectProvider<SqsClient> sqsClient;

    public ReportJobQueue(
            ReportExportProperties properties,
            @Qualifier("reportSqsClient") ObjectProvider<SqsClient> sqsClient) {
        this.properties = properties;
        this.sqsClient = sqsClient;
    }

    public void publish(UUID jobId) {
        if (!properties.getSqs().isProducerEnabled()) return;
        SqsClient client = requireClient();
        client.sendMessage(SendMessageRequest.builder()
                .queueUrl(requireQueueUrl())
                .messageBody(jobId.toString())
                .build());
    }

    public List<Delivery> receive() {
        if (!properties.getSqs().isConsumerEnabled()) return List.of();
        List<Message> messages = requireClient()
                .receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(requireQueueUrl())
                        .waitTimeSeconds(properties.getSqs().getWaitSeconds())
                        .maxNumberOfMessages(properties.getBatchSize())
                        .visibilityTimeout((int) Math.max(
                                60,
                                Math.min(
                                        properties.getStaleAfter().toSeconds(),
                                        43_200)))
                        .build())
                .messages();
        List<Delivery> deliveries = new ArrayList<>(messages.size());
        for (Message message : messages) {
            try {
                deliveries.add(new Delivery(
                        UUID.fromString(message.body()), message.receiptHandle()));
            } catch (IllegalArgumentException exception) {
                log.warn("Discarding invalid report job queue message id={}", message.messageId());
                acknowledge(new Delivery(null, message.receiptHandle()));
            }
        }
        return deliveries;
    }

    public void acknowledge(Delivery delivery) {
        if (!properties.getSqs().isConsumerEnabled()) return;
        requireClient().deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(requireQueueUrl())
                .receiptHandle(delivery.receiptHandle())
                .build());
    }

    private SqsClient requireClient() {
        SqsClient client = sqsClient.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("Report SQS is enabled but its client is unavailable");
        }
        return client;
    }

    private String requireQueueUrl() {
        String queueUrl = properties.getSqs().getQueueUrl();
        if (queueUrl.isBlank()) {
            throw new IllegalStateException(
                    "Report SQS is enabled but app.reports.sqs.queue-url is empty");
        }
        return queueUrl;
    }

    public record Delivery(UUID jobId, String receiptHandle) {}
}
