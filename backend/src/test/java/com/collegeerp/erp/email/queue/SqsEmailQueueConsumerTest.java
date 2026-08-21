package com.collegeerp.erp.email.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.collegeerp.erp.email.config.MailProperties;
import com.collegeerp.erp.email.entity.EmailNotification;
import com.collegeerp.erp.email.enums.EmailStatus;
import com.collegeerp.erp.email.worker.EmailClaimResult;
import com.collegeerp.erp.email.worker.EmailClaimService;
import com.collegeerp.erp.email.worker.EmailDeliveryOutcome;
import com.collegeerp.erp.email.worker.EmailWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsEmailQueueConsumerTest {

    @Mock private SqsClient sqsClient;
    @Mock private EmailClaimService claimService;
    @Mock private EmailWorker worker;

    private SqsEmailQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.getSqs().setQueueUrl(
                "https://sqs.ap-south-1.amazonaws.com/123456789012/email");
        consumer = new SqsEmailQueueConsumer(
                sqsClient,
                properties,
                new EmailQueueMessageCodec(new ObjectMapper()),
                claimService,
                worker,
                Runnable::run);
    }

    @Test
    void receivesDeliversAndAcknowledgesOnlyAfterSentTransition() {
        EmailNotification notification = new EmailNotification();
        when(claimService.claimById(42L))
                .thenReturn(EmailClaimResult.claimed(notification));
        when(worker.process(notification))
                .thenReturn(new EmailDeliveryOutcome(EmailStatus.SENT, 0));
        arrangeReceive(validMessage());

        consumer.poll();

        verify(worker).process(notification);
        ArgumentCaptor<DeleteMessageRequest> delete =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(delete.capture());
        assertEquals("receipt-1", delete.getValue().receiptHandle());
    }

    @Test
    void providerFailureDefersWithoutAcknowledging() {
        EmailNotification notification = new EmailNotification();
        when(claimService.claimById(42L))
                .thenReturn(EmailClaimResult.claimed(notification));
        when(worker.process(notification))
                .thenReturn(new EmailDeliveryOutcome(
                        EmailStatus.RETRY_PENDING, 75));
        arrangeReceive(validMessage());

        consumer.poll();

        ArgumentCaptor<ChangeMessageVisibilityRequest> visibility =
                ArgumentCaptor.forClass(ChangeMessageVisibilityRequest.class);
        verify(sqsClient).changeMessageVisibility(visibility.capture());
        assertEquals(75, visibility.getValue().visibilityTimeout());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void terminalFailureIsNotAcknowledgedSoQueueCanRedriveToDlq() {
        EmailNotification notification = new EmailNotification();
        when(claimService.claimById(42L))
                .thenReturn(EmailClaimResult.claimed(notification));
        when(worker.process(notification))
                .thenReturn(new EmailDeliveryOutcome(EmailStatus.FAILED, 0));
        arrangeReceive(validMessage());

        consumer.poll();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(sqsClient, never())
                .changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
    }

    @Test
    void duplicateForAlreadySentRowIsAcknowledgedWithoutSendingAgain() {
        when(claimService.claimById(42L))
                .thenReturn(EmailClaimResult.acknowledge());
        arrangeReceive(validMessage());

        consumer.poll();

        verify(worker, never()).process(any());
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void activeClaimIsDeferredAndInvalidPayloadIsLeftForDlq() {
        when(claimService.claimById(42L))
                .thenReturn(EmailClaimResult.retryLater(300));
        arrangeReceive(validMessage());

        consumer.poll();

        verify(sqsClient).changeMessageVisibility(
                any(ChangeMessageVisibilityRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));

        Message invalid = Message.builder()
                .messageId("message-2")
                .receiptHandle("receipt-2")
                .body("{\"notificationId\":42,\"email\":\"student@example.com\"}")
                .build();
        arrangeReceive(invalid);
        consumer.poll();

        verify(claimService).claimById(42L);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    private void arrangeReceive(Message message) {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
    }

    private Message validMessage() {
        return Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{\"notificationId\":42}")
                .build();
    }
}
