package com.collegeerp.erp.reports.repository;

import com.collegeerp.erp.reports.entity.ReportExportJob;
import com.collegeerp.erp.reports.enums.ReportExportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportExportJobRepository extends JpaRepository<ReportExportJob, UUID> {

    Optional<ReportExportJob> findByRequesterUserIdAndIdempotencyKeyHash(
            Long requesterUserId, String idempotencyKeyHash);

    Optional<ReportExportJob> findByIdAndRequesterUserId(UUID id, Long requesterUserId);

    long countByRequesterUserIdAndStatusIn(
            Long requesterUserId, Collection<ReportExportStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ReportExportJob job where job.id = :id")
    Optional<ReportExportJob> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            select *
            from report_export_jobs
            where status = 'QUEUED'
               or (status = 'RETRY_PENDING' and next_attempt_at <= :now)
            order by created_at
            for update skip locked
            limit :limit
            """, nativeQuery = true)
    List<ReportExportJob> lockEligible(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    List<ReportExportJob> findByStatusAndProcessingStartedAtBefore(
            ReportExportStatus status, LocalDateTime staleBefore);

    List<ReportExportJob> findByStatusAndExpiresAtBefore(
            ReportExportStatus status, LocalDateTime expiresBefore);
}
