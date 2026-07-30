package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.TooManyRequestsException;
import com.jadhavr.erp.reports.config.ReportExportProperties;
import com.jadhavr.erp.reports.dto.CreateReportExportRequest;
import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.enums.ReportExportStatus;
import com.jadhavr.erp.reports.enums.ReportExportType;
import com.jadhavr.erp.reports.repository.ReportExportJobRepository;
import com.jadhavr.erp.reports.transport.ReportJobQueue;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.storage.PresignedObjectStorageService;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportJobServiceTest {
    @Mock private ReportExportJobRepository jobs;
    @Mock private StaffProfileRepository staff;
    @Mock private ReportCsvExportService csv;
    @Mock private ReportJobQueue queue;
    @Mock private ObjectProvider<PresignedObjectStorageService> storageProvider;
    @Mock private PresignedObjectStorageService presignedStorage;
    @Mock private AuditLogService audit;

    private ReportExportJobService service;

    @BeforeEach
    void setUp() {
        ReportExportProperties properties = new ReportExportProperties();
        ReportExportScopeService scopes = new ReportExportScopeService(staff);
        service = new ReportExportJobService(
                jobs, scopes, csv, properties, queue, storageProvider, audit);
        authenticate(RoleName.PRINCIPAL, 10L, 7L);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exactDuplicateRequestReusesOneJob() {
        AtomicReference<ReportExportJob> stored = new AtomicReference<>();
        when(csv.validateStatusFilter(ReportExportType.STUDENTS, "active"))
                .thenReturn("ACTIVE");
        when(jobs.findByRequesterUserIdAndIdempotencyKeyHash(anyLong(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(jobs.save(any(ReportExportJob.class))).thenAnswer(invocation -> {
            ReportExportJob job = invocation.getArgument(0);
            job.setCreatedAt(LocalDateTime.now());
            stored.set(job);
            return job;
        });
        CreateReportExportRequest request = new CreateReportExportRequest(null, null, "active");

        var first = service.create("students", "report-key-001", request);
        var duplicate = service.create("students", "report-key-001", request);

        assertFalse(first.reused());
        assertTrue(duplicate.reused());
        assertEquals(first.job().id(), duplicate.job().id());
        verify(queue).publish(first.job().id());
    }

    @Test
    void reusingKeyForDifferentScopeIsRejected() {
        AtomicReference<ReportExportJob> stored = new AtomicReference<>();
        when(csv.validateStatusFilter(ReportExportType.STUDENTS, null)).thenReturn(null);
        when(jobs.findByRequesterUserIdAndIdempotencyKeyHash(anyLong(), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(jobs.save(any(ReportExportJob.class))).thenAnswer(invocation -> {
            ReportExportJob job = invocation.getArgument(0);
            stored.set(job);
            return job;
        });

        service.create(
                "students",
                "report-key-002",
                new CreateReportExportRequest(null, 20L, null));

        assertThrows(
                DuplicateResourceException.class,
                () -> service.create(
                        "students",
                        "report-key-002",
                        new CreateReportExportRequest(null, 21L, null)));
    }

    @Test
    void activeJobLimitProvidesBackpressure() {
        when(csv.validateStatusFilter(ReportExportType.STUDENTS, null)).thenReturn(null);
        when(jobs.findByRequesterUserIdAndIdempotencyKeyHash(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(jobs.countByRequesterUserIdAndStatusIn(anyLong(), any())).thenReturn(3L);

        assertThrows(
                TooManyRequestsException.class,
                () -> service.create(
                        "students",
                        "report-key-003",
                        new CreateReportExportRequest(null, null, null)));
        verify(jobs, never()).save(any());
    }

    @Test
    void completedOwnerGetsOnlyShortLivedPresignedDownload() {
        ReportExportJob job = completedJob();
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        when(storageProvider.getIfAvailable()).thenReturn(presignedStorage);
        when(presignedStorage.presignDownload(
                        anyString(), anyString(), anyString(), any()))
                .thenReturn(new PresignedObjectStorageService.PresignedDownload(
                        "https://private.example/download", Instant.parse("2026-07-28T13:00:00Z")));

        var response = service.download(job.getId());

        assertEquals("https://private.example/download", response.url());
        verify(presignedStorage).presignDownload(
                job.getResultKey(),
                job.getResultFilename(),
                job.getResultContentType(),
                java.time.Duration.ofMinutes(2));
    }

    @Test
    void anotherUserCannotReadStatusOrCreateDownload() {
        ReportExportJob job = completedJob();
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        authenticate(RoleName.PRINCIPAL, 11L, 7L);

        assertThrows(AccessDeniedException.class, () -> service.status(job.getId()));
        assertThrows(AccessDeniedException.class, () -> service.download(job.getId()));
        verify(storageProvider, never()).getIfAvailable();
    }

    private ReportExportJob completedJob() {
        ReportExportJob job = new ReportExportJob();
        job.setRequesterUserId(10L);
        job.setRequesterCollegeId(7L);
        job.setScopeCollegeId(7L);
        job.setRequesterRole("PRINCIPAL");
        job.setReportType(ReportExportType.STUDENTS);
        job.setStatus(ReportExportStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now().minusMinutes(1));
        job.setExpiresAt(LocalDateTime.now().plusHours(1));
        job.setResultKey("colleges/7/report-exports/" + job.getId() + "/students.csv");
        job.setResultFilename("students.csv");
        job.setResultContentType(ReportCsvExportService.CSV_CONTENT_TYPE);
        job.setResultSizeBytes(10L);
        return job;
    }

    private void authenticate(RoleName role, Long userId, Long collegeId) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, userId, collegeId));
    }
}
