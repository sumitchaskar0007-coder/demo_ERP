package com.jadhavr.erp.reports.transport;

import com.jadhavr.erp.reports.config.ReportExportProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportJobQueueTest {
    @Mock private ObjectProvider<SqsClient> clientProvider;
    @Mock private SqsClient client;

    @Test
    void producerSendsOnlyOpaqueJobIdWhileConsumerRemainsDisabled() {
        ReportExportProperties properties = new ReportExportProperties();
        properties.getSqs().setProducerEnabled(true);
        properties.getSqs().setConsumerEnabled(false);
        properties.getSqs().setQueueUrl("https://sqs.ap-south-1.amazonaws.com/123/reports");
        when(clientProvider.getIfAvailable()).thenReturn(client);
        ReportJobQueue queue = new ReportJobQueue(properties, clientProvider);
        UUID jobId = UUID.randomUUID();

        queue.publish(jobId);
        assertTrue(queue.receive().isEmpty());

        ArgumentCaptor<SendMessageRequest> request =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(request.capture());
        assertEquals(jobId.toString(), request.getValue().messageBody());
        verify(client, never()).receiveMessage(
                org.mockito.ArgumentMatchers.any(
                        software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.class));
    }
}
