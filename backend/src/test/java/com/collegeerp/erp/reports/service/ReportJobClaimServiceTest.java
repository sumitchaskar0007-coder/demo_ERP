package com.collegeerp.erp.reports.service;

import com.collegeerp.erp.reports.config.ReportExportProperties;
import com.collegeerp.erp.reports.entity.ReportExportJob;
import com.collegeerp.erp.reports.enums.ReportExportStatus;
import com.collegeerp.erp.reports.repository.ReportExportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportJobClaimServiceTest {
    @Mock private ReportExportJobRepository jobs;

    @Test
    void failedAttemptMovesToRetryWithoutExposingException() {
        ReportExportJob job = processingJob(1, 3);
        when(jobs.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        ReportJobClaimService service =
                new ReportJobClaimService(jobs, new ReportExportProperties());

        service.fail(job.getId());

        assertEquals(ReportExportStatus.RETRY_PENDING, job.getStatus());
        assertEquals("Report generation failed", job.getFailureReason());
        assertNotNull(job.getNextAttemptAt());
        assertNull(job.getProcessingStartedAt());
    }

    @Test
    void finalFailedAttemptBecomesTerminal() {
        ReportExportJob job = processingJob(3, 3);
        when(jobs.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        ReportJobClaimService service =
                new ReportJobClaimService(jobs, new ReportExportProperties());

        service.fail(job.getId());

        assertEquals(ReportExportStatus.FAILED, job.getStatus());
        assertNull(job.getNextAttemptAt());
    }

    @Test
    void completedResultGetsExpiryAndCleanupRemovesObjectReference() {
        ReportExportJob job = processingJob(1, 3);
        when(jobs.findByIdForUpdate(job.getId())).thenReturn(Optional.of(job));
        ReportJobClaimService service =
                new ReportJobClaimService(jobs, new ReportExportProperties());

        service.complete(
                job.getId(),
                "colleges/7/report-exports/id/students.csv",
                "students.csv",
                ReportCsvExportService.CSV_CONTENT_TYPE,
                42L);

        assertEquals(ReportExportStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getExpiresAt());
        assertEquals(42L, job.getResultSizeBytes());

        job.setExpiresAt(java.time.LocalDateTime.now().minusSeconds(1));
        service.markExpired(job.getId());

        assertEquals(ReportExportStatus.EXPIRED, job.getStatus());
        assertNull(job.getResultKey());
        assertNull(job.getResultSizeBytes());
    }

    private ReportExportJob processingJob(int attempt, int maximum) {
        ReportExportJob job = new ReportExportJob();
        job.setStatus(ReportExportStatus.PROCESSING);
        job.setAttemptCount(attempt);
        job.setMaxAttempts(maximum);
        job.setProcessingStartedAt(java.time.LocalDateTime.now());
        return job;
    }
}
