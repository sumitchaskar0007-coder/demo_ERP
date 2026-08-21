package com.collegeerp.erp.email.queue;

public interface EmailQueuePublisher {
    void publish(long notificationId);
}
