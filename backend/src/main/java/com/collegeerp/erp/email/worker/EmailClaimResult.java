package com.collegeerp.erp.email.worker;

import com.collegeerp.erp.email.entity.EmailNotification;

public record EmailClaimResult(
        Action action,
        EmailNotification notification,
        int retryAfterSeconds) {

    public enum Action {
        CLAIMED,
        ACKNOWLEDGE,
        RETRY_LATER,
        DEAD_LETTER
    }

    public static EmailClaimResult claimed(EmailNotification notification) {
        return new EmailClaimResult(Action.CLAIMED, notification, 0);
    }

    public static EmailClaimResult acknowledge() {
        return new EmailClaimResult(Action.ACKNOWLEDGE, null, 0);
    }

    public static EmailClaimResult retryLater(int seconds) {
        return new EmailClaimResult(Action.RETRY_LATER, null, Math.max(1, seconds));
    }

    public static EmailClaimResult deadLetter() {
        return new EmailClaimResult(Action.DEAD_LETTER, null, 0);
    }
}
