package com.jadhavr.erp.email.queue;

public interface EmailQueuePublisher {
    void publish(long notificationId);
}
