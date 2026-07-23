package com.jadhavr.erp.auth.repository;

import com.jadhavr.erp.auth.entity.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long> {
    List<SecurityAuditEvent> findByInstitutionIdAndEventTypeAndSuccessTrueAndCreatedAtBetween(
            Long institutionId, String eventType, LocalDateTime from, LocalDateTime to);

    @Query("""
            select event.userId, max(event.createdAt)
            from SecurityAuditEvent event
            where event.institutionId = :institutionId
              and event.eventType = 'LOGIN_SUCCESS'
              and event.success = true
              and event.userId in :userIds
            group by event.userId
            """)
    List<Object[]> findLatestSuccessfulLogins(
            @Param("institutionId") Long institutionId,
            @Param("userIds") Collection<Long> userIds);
}
