package com.jadhavr.erp.email.worker;

import com.jadhavr.erp.email.enums.EmailStatus;

public record EmailDeliveryOutcome(EmailStatus status, int retryAfterSeconds) {

    public boolean wasSent() {
        return status == EmailStatus.SENT;
    }

    public boolean shouldRetry() {
        return status == EmailStatus.RETRY_PENDING;
    }
}
