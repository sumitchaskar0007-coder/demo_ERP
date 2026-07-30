package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.reports.config.ReportExportProperties;
import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.enums.ReportExportStatus;
import com.jadhavr.erp.reports.repository.ReportExportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportJobClaimService {
    private final ReportExportJobRepository jobs;
    private final ReportExportProperties properties;

    public ReportJobClaimService(
            ReportExportJobRepository jobs, ReportExportProperties properties) {
        this.jobs = jobs;
        this.properties = properties;
    }

    @Transactional
    public List<ReportExportJob> claimBatch() {
        LocalDateTime now = LocalDateTime.now();
        List<ReportExportJob> claimed = jobs.lockEligible(now, properties.getBatchSize());
        claimed.forEach(job -> markProcessing(job, now));
        return jobs.saveAll(claimed);
    }

    @Transactional
    public Optional<ReportExportJob> claim(UUID jobId) {
        LocalDateTime now = LocalDateTime.now();
        return jobs.findByIdForUpdate(jobId)
                .filter(job -> eligible(job, now))
                .map(job -> {
                    markProcessing(job, now);
                    return jobs.save(job);
                });
    }

    @Transactional
    public void complete(
            UUID jobId,
            String resultKey,
            String resultFilename,
            String contentType,
            long resultSize) {
        ReportExportJob job = jobs.findByIdForUpdate(jobId)
                .orElseThrow(() -> new IllegalStateException("Report export job disappeared"));
        if (job.getStatus() != ReportExportStatus.PROCESSING) {
            throw new IllegalStateException("Report export job is not being processed");
        }
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ReportExportStatus.COMPLETED);
        job.setCompletedAt(now);
        job.setExpiresAt(now.plus(properties.getResultTtl()));
        job.setResultKey(resultKey);
        job.setResultFilename(resultFilename);
        job.setResultContentType(contentType);
        job.setResultSizeBytes(resultSize);
        job.setProcessingStartedAt(null);
        job.setNextAttemptAt(null);
        job.setFailureReason(null);
        jobs.save(job);
    }

    @Transactional
    public void fail(UUID jobId) {
        jobs.findByIdForUpdate(jobId).ifPresent(job -> {
            if (job.getStatus() != ReportExportStatus.PROCESSING) return;
            job.setProcessingStartedAt(null);
            job.setFailureReason("Report generation failed");
            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                job.setStatus(ReportExportStatus.FAILED);
                job.setNextAttemptAt(null);
            } else {
                job.setStatus(ReportExportStatus.RETRY_PENDING);
                long multiplier = 1L << Math.min(job.getAttemptCount() - 1, 5);
                job.setNextAttemptAt(
                        LocalDateTime.now().plus(properties.getRetryDelay().multipliedBy(multiplier)));
            }
            jobs.save(job);
        });
    }

    @Transactional
    public void recoverStale() {
        LocalDateTime staleBefore = LocalDateTime.now().minus(properties.getStaleAfter());
        List<ReportExportJob> stale = jobs.findByStatusAndProcessingStartedAtBefore(
                ReportExportStatus.PROCESSING, staleBefore);
        for (ReportExportJob job : stale) {
            job.setProcessingStartedAt(null);
            job.setFailureReason("Report generation was interrupted");
            if (job.getAttemptCount() >= job.getMaxAttempts()) {
                job.setStatus(ReportExportStatus.FAILED);
                job.setNextAttemptAt(null);
            } else {
                job.setStatus(ReportExportStatus.RETRY_PENDING);
                job.setNextAttemptAt(LocalDateTime.now());
            }
        }
        jobs.saveAll(stale);
    }

    @Transactional(readOnly = true)
    public List<ReportExportJob> expiredCompletedJobs() {
        return jobs.findByStatusAndExpiresAtBefore(
                ReportExportStatus.COMPLETED, LocalDateTime.now());
    }

    @Transactional
    public void markExpired(UUID jobId) {
        jobs.findByIdForUpdate(jobId).ifPresent(job -> {
            if (job.getStatus() != ReportExportStatus.COMPLETED
                    || job.getExpiresAt() == null
                    || job.getExpiresAt().isAfter(LocalDateTime.now())) {
                return;
            }
            job.setStatus(ReportExportStatus.EXPIRED);
            job.setResultKey(null);
            job.setResultFilename(null);
            job.setResultContentType(null);
            job.setResultSizeBytes(null);
            jobs.save(job);
        });
    }

    private boolean eligible(ReportExportJob job, LocalDateTime now) {
        return job.getStatus() == ReportExportStatus.QUEUED
                || (job.getStatus() == ReportExportStatus.RETRY_PENDING
                        && job.getNextAttemptAt() != null
                        && !job.getNextAttemptAt().isAfter(now));
    }

    private void markProcessing(ReportExportJob job, LocalDateTime now) {
        job.setStatus(ReportExportStatus.PROCESSING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setProcessingStartedAt(now);
        job.setNextAttemptAt(null);
    }
}
