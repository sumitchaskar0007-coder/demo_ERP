package com.jadhavr.erp.email.service;

import com.jadhavr.erp.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmailNotificationService {
    void queueUserCreatedEmail(User user);
    void queueUserCreatedEmail(User user, String temporaryPassword);
    void queuePrincipalCreatedEmail(User user);
    void queuePrincipalCreatedEmail(User user, String temporaryPassword);
    void queueAdmissionCompletedEmail(User user, String admissionReferenceNumber);
    void queueAdmissionApprovedEmail(
            User user, String admissionReferenceNumber, String admissionNumber);
    void queueAccountActivatedEmail(User user);
    void queueAccountDeactivatedEmail(User user);
    void queuePasswordResetEmail(User user, String token, LocalDateTime expires);
    void queuePasswordChangedEmail(User user);
    void queueEmailVerificationEmail(User user, String token, LocalDateTime expires);
    void queueEmailVerifiedEmail(User user);
    void queueFeePaymentReminder(
            User user, Long accountId, String admissionNumber, BigDecimal remainingAmount);
}
