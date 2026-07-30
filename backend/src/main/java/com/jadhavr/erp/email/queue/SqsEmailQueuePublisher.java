package com.jadhavr.erp.email.queue;

import com.jadhavr.erp.email.config.MailProperties;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsEmailQueuePublisher implements EmailQueuePublisher {

    private final SqsClient sqsClient;
    private final MailProperties properties;
    private final EmailQueueMessageCodec codec;

    public SqsEmailQueuePublisher(
            SqsClient sqsClient,
            MailProperties properties,
            EmailQueueMessageCodec codec) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.codec = codec;
    }

    @Override
    public void publish(long notificationId) {
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(properties.getSqs().getQueueUrl())
                .messageBody(codec.encode(notificationId))
                .build());
    }
}
