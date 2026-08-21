package com.collegeerp.erp.email.service;

import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.email.dto.EmailNotificationResponse;
import com.collegeerp.erp.email.entity.EmailNotification;
import com.collegeerp.erp.email.enums.EmailStatus;
import com.collegeerp.erp.email.enums.EmailType;
import com.collegeerp.erp.email.queue.EmailQueueDispatcher;
import com.collegeerp.erp.email.repository.EmailNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class EmailAdminService {

    private final EmailNotificationRepository repository;
    private final EmailQueueDispatcher queueDispatcher;

    public EmailAdminService(
            EmailNotificationRepository repository,
            EmailQueueDispatcher queueDispatcher) {
        this.repository = repository;
        this.queueDispatcher = queueDispatcher;
    }

    public PageResponse<EmailNotificationResponse> search(
            EmailStatus status,
            EmailType type,
            String recipient,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination");
        }
        Specification<EmailNotification> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (type != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("emailType"), type));
        }
        if (recipient != null && !recipient.isBlank()) {
            String pattern = "%" + recipient.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and(
                    (root, query, builder) ->
                            builder.like(builder.lower(root.get("recipientEmail")), pattern));
        }
        return PageResponse.from(repository.findAll(
                        specification,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::map));
    }

    public EmailNotificationResponse get(Long id) {
        return map(find(id));
    }

    @Transactional
    public EmailNotificationResponse retry(Long id) {
        EmailNotification notification = find(id);
        if (notification.getStatus() == EmailStatus.SENT
                || notification.getStatus() == EmailStatus.CANCELLED) {
            throw new IllegalArgumentException("Notification cannot be retried");
        }
        notification.setStatus(EmailStatus.QUEUED);
        notification.setNextRetryAt(null);
        notification.setProcessingStartedAt(null);
        notification.setFailureReason(null);
        notification.setRetryCount(0);
        EmailNotification saved = repository.save(notification);
        queueDispatcher.publishAfterCommit(saved.getId());
        return map(saved);
    }

    @Transactional
    public EmailNotificationResponse cancel(Long id) {
        EmailNotification notification = find(id);
        if (notification.getStatus() == EmailStatus.SENT) {
            throw new IllegalArgumentException("Sent notification cannot be cancelled");
        }
        notification.setStatus(EmailStatus.CANCELLED);
        return map(repository.save(notification));
    }

    private EmailNotification find(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Email notification not found"));
    }

    private EmailNotificationResponse map(EmailNotification notification) {
        String email = notification.getRecipientEmail();
        int at = email.indexOf('@');
        String masked = at > 1 ? email.charAt(0) + "***" + email.substring(at) : "***";
        return new EmailNotificationResponse(
                notification.getId(),
                masked,
                notification.getRecipientName(),
                notification.getEmailType(),
                notification.getSubject(),
                notification.getStatus(),
                notification.getPriority(),
                notification.getRetryCount(),
                notification.getMaxRetries(),
                notification.getProvider(),
                notification.getFailureReason(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getCorrelationId());
    }
}
