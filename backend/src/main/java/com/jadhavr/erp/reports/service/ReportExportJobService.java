package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.audit.enums.AuditAction;
import com.jadhavr.erp.audit.enums.AuditModule;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.common.exception.TooManyRequestsException;
import com.jadhavr.erp.reports.config.ReportExportProperties;
import com.jadhavr.erp.reports.dto.CreateReportExportRequest;
import com.jadhavr.erp.reports.dto.ReportExportDownloadResponse;
import com.jadhavr.erp.reports.dto.ReportExportJobResponse;
import com.jadhavr.erp.reports.entity.ReportExportJob;
import com.jadhavr.erp.reports.enums.ReportExportStatus;
import com.jadhavr.erp.reports.enums.ReportExportType;
import com.jadhavr.erp.reports.repository.ReportExportJobRepository;
import com.jadhavr.erp.reports.transport.ReportJobQueue;
import com.jadhavr.erp.storage.PresignedObjectStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReportExportJobService {
    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{7,127}");

    private final ReportExportJobRepository jobs;
    private final ReportExportScopeService scopes;
    private final ReportCsvExportService csv;
    private final ReportExportProperties properties;
    private final ReportJobQueue queue;
    private final ObjectProvider<PresignedObjectStorageService> presignedStorage;
    private final AuditLogService audit;

    public ReportExportJobService(
            ReportExportJobRepository jobs,
            ReportExportScopeService scopes,
            ReportCsvExportService csv,
            ReportExportProperties properties,
            ReportJobQueue queue,
            ObjectProvider<PresignedObjectStorageService> presignedStorage,
            AuditLogService audit) {
        this.jobs = jobs;
        this.scopes = scopes;
        this.csv = csv;
        this.properties = properties;
        this.queue = queue;
        this.presignedStorage = presignedStorage;
        this.audit = audit;
    }

    @Transactional
    public CreateResult create(
            String typeValue, String idempotencyKey, CreateReportExportRequest request) {
        if (!properties.isEnabled()) {
            throw new BadRequestException("Asynchronous report exports are disabled");
        }
        ReportExportType type = ReportExportType.fromPath(typeValue);
        String key = requireIdempotencyKey(idempotencyKey);
        String statusFilter = csv.validateStatusFilter(type, request.status());
        ReportExportScopeService.CapturedScope scope =
                scopes.capture(type, request.collegeId(), request.departmentId());
        String keyHash = hash(scope.requesterUserId() + ":" + key);
        String fingerprint = hash(String.join(
                "|",
                type.name(),
                scope.collegeId().toString(),
                Objects.toString(scope.departmentId(), ""),
                Objects.toString(statusFilter, "")));

        var existing = jobs.findByRequesterUserIdAndIdempotencyKeyHash(
                scope.requesterUserId(), keyHash);
        if (existing.isPresent()) {
            ReportExportJob job = existing.get();
            if (!job.getRequestFingerprint().equals(fingerprint)) {
                throw new DuplicateResourceException(
                        "Idempotency-Key was already used for a different report request");
            }
            return new CreateResult(ReportExportJobResponse.from(job), true);
        }
        long active = jobs.countByRequesterUserIdAndStatusIn(
                scope.requesterUserId(),
                Set.of(
                        ReportExportStatus.QUEUED,
                        ReportExportStatus.PROCESSING,
                        ReportExportStatus.RETRY_PENDING));
        if (active >= properties.getMaxActivePerUser()) {
            throw new TooManyRequestsException(
                    "Too many active report exports; wait for an existing job to finish", 30);
        }

        ReportExportJob job = new ReportExportJob();
        job.setRequesterUserId(scope.requesterUserId());
        job.setRequesterCollegeId(scope.requesterCollegeId());
        job.setScopeCollegeId(scope.collegeId());
        job.setScopeDepartmentId(scope.departmentId());
        job.setRequesterRole(scope.requesterRole());
        job.setReportType(type);
        job.setStatusFilter(statusFilter);
        job.setIdempotencyKeyHash(keyHash);
        job.setRequestFingerprint(fingerprint);
        job.setMaxAttempts(properties.getMaxAttempts());
        jobs.save(job);
        publishAfterCommit(job.getId());
        audit.log(
                AuditModule.REPORT,
                AuditAction.EXPORT,
                "ReportExportJob",
                null,
                "Queued " + type.pathValue() + " report export");
        return new CreateResult(ReportExportJobResponse.from(job), false);
    }

    @Transactional(readOnly = true)
    public ReportExportJobResponse status(UUID jobId) {
        ReportExportJob job = owned(jobId);
        return ReportExportJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public ReportExportDownloadResponse download(UUID jobId) {
        ReportExportJob job = owned(jobId);
        if (job.getStatus() == ReportExportStatus.EXPIRED
                || (job.getExpiresAt() != null
                        && !job.getExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new ResourceNotFoundException("Report export has expired");
        }
        if (job.getStatus() != ReportExportStatus.COMPLETED
                || job.getResultKey() == null
                || job.getResultFilename() == null) {
            throw new BadRequestException("Report export is not ready for download");
        }
        PresignedObjectStorageService storage = presignedStorage.getIfAvailable();
        if (storage == null) {
            throw new BadRequestException("Report downloads are temporarily unavailable");
        }
        Duration remaining = Duration.between(LocalDateTime.now(), job.getExpiresAt());
        Duration linkExpiry = remaining.compareTo(properties.getDownloadExpiry()) < 0
                ? remaining : properties.getDownloadExpiry();
        var download = storage.presignDownload(
                job.getResultKey(),
                job.getResultFilename(),
                job.getResultContentType(),
                linkExpiry);
        audit.log(
                AuditModule.REPORT,
                AuditAction.EXPORT,
                "ReportExportJob",
                null,
                "Created a short-lived private report download link");
        return new ReportExportDownloadResponse(download.url(), download.expiresAt());
    }

    private ReportExportJob owned(UUID jobId) {
        ReportExportJob job = jobs.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Report export job not found"));
        scopes.requireCurrentOwner(job);
        return job;
    }

    private void publishAfterCommit(UUID jobId) {
        Runnable publish = () -> {
            try {
                queue.publish(jobId);
            } catch (RuntimeException exception) {
                // The database row is the durable fallback. Do not turn a committed,
                // recoverable job into an API failure because SQS is temporarily down.
                org.slf4j.LoggerFactory.getLogger(ReportExportJobService.class)
                        .warn("Unable to publish report job {}; database polling will recover it",
                                jobId, exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish.run();
                        }
                    });
        } else {
            publish.run();
        }
    }

    private String requireIdempotencyKey(String value) {
        String key = value == null ? "" : value.trim();
        if (!IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new BadRequestException(
                    "Idempotency-Key must be 8-128 characters using letters, numbers, '.', '_', ':' or '-'");
        }
        return key;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record CreateResult(ReportExportJobResponse job, boolean reused) {}
}
