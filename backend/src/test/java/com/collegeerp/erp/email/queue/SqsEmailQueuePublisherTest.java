package com.collegeerp.erp.email.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.email.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsEmailQueuePublisherTest {

    @Mock private SqsClient sqsClient;

    @Test
    void publishesStrictIdOnlyPayloadWithoutEmailContentOrPii() {
        MailProperties properties = new MailProperties();
        properties.getSqs().setQueueUrl(
                "https://sqs.ap-south-1.amazonaws.com/123456789012/email");
        SqsEmailQueuePublisher publisher = new SqsEmailQueuePublisher(
                sqsClient,
                properties,
                new EmailQueueMessageCodec(new ObjectMapper()));

        publisher.publish(42L);

        ArgumentCaptor<SendMessageRequest> request =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(request.capture());
        assertEquals("{\"notificationId\":42}", request.getValue().messageBody());
        assertFalse(request.getValue().messageBody().contains("@"));
        assertFalse(request.getValue().messageBody().contains("subject"));
        assertFalse(request.getValue().messageBody().contains("template"));
    }
}
