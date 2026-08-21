package com.collegeerp.erp.reports.worker;

import com.collegeerp.erp.reports.config.ReportExportProperties;
import com.collegeerp.erp.reports.entity.ReportExportJob;
import com.collegeerp.erp.reports.enums.ReportExportStatus;
import com.collegeerp.erp.reports.enums.ReportExportType;
import com.collegeerp.erp.reports.service.ReportCsvExportService;
import com.collegeerp.erp.reports.service.ReportJobClaimService;
import com.collegeerp.erp.reports.transport.ReportJobQueue;
import com.collegeerp.erp.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportWorkerTest {
    @Mock private ReportJobClaimService claims;
    @Mock private ReportCsvExportService csv;
    @Mock private ObjectStorageService storage;
    @Mock private ReportJobQueue queue;

    @Test
    void storesCompletedResultUnderPrivateTenantPrefix() {
        ReportExportJob job = job();
        byte[] content = "header\nrow\n".getBytes(StandardCharsets.UTF_8);
        when(csv.generate(ReportExportType.STUDENTS, 7L, 20L, "ACTIVE"))
                .thenReturn(new ReportCsvExportService.GeneratedReport(content, 1));
        ReportExportWorker worker = worker();

        worker.process(job);

        String key = "colleges/7/report-exports/" + job.getId() + "/students.csv";
        verify(storage).put(key, content, ReportCsvExportService.CSV_CONTENT_TYPE);
        verify(claims).complete(
                eq(job.getId()),
                eq(key),
                eq("students-report-" + job.getId() + ".csv"),
                eq(ReportCsvExportService.CSV_CONTENT_TYPE),
                eq((long) content.length));
    }

    @Test
    void generationFailureIsHandedToDurableRetryLifecycle() {
        ReportExportJob job = job();
        when(csv.generate(ReportExportType.STUDENTS, 7L, 20L, "ACTIVE"))
                .thenThrow(new IllegalStateException("database unavailable with private detail"));
        ReportExportWorker worker = worker();

        worker.process(job);

        verify(claims).fail(job.getId());
    }

    private ReportExportWorker worker() {
        return new ReportExportWorker(
                new ReportExportProperties(), claims, csv, storage, queue, Runnable::run);
    }

    private ReportExportJob job() {
        ReportExportJob job = new ReportExportJob();
        job.setStatus(ReportExportStatus.PROCESSING);
        job.setAttemptCount(1);
        job.setMaxAttempts(3);
        job.setScopeCollegeId(7L);
        job.setScopeDepartmentId(20L);
        job.setReportType(ReportExportType.STUDENTS);
        job.setStatusFilter("ACTIVE");
        return job;
    }
}
