package com.jadhavr.erp.email.repository;

import com.jadhavr.erp.email.entity.EmailNotification;
import com.jadhavr.erp.email.enums.EmailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailNotificationRepository
        extends JpaRepository<EmailNotification, Long>,
                JpaSpecificationExecutor<EmailNotification> {

    boolean existsByIdempotencyKey(String key);

    @Query(
            value = """
                    select *
                    from email_notifications
                    where status = 'QUEUED'
                       or (status = 'RETRY_PENDING' and next_retry_at <= now())
                    order by priority, created_at
                    for update skip locked
                    limit :limit
                    """,
            nativeQuery = true)
    List<EmailNotification> lockEligible(@Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from EmailNotification notification where notification.id = :id")
    Optional<EmailNotification> findByIdForUpdate(@Param("id") long id);

    @Query("""
            select notification.id
            from EmailNotification notification
            where notification.status = :status
              and notification.createdAt <= :cutoff
              and notification.id > :afterId
            order by notification.id
            """)
    List<Long> findIdsByStatusCreatedBeforeAndIdAfter(
            @Param("status") EmailStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("afterId") long afterId,
            Pageable pageable);

    List<EmailNotification> findByStatusAndProcessingStartedAtBefore(
            EmailStatus status, LocalDateTime processingStartedAt);
}
