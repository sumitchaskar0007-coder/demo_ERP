package com.collegeerp.erp.reports.entity;

import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.reports.enums.ReportExportStatus;
import com.collegeerp.erp.reports.enums.ReportExportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "report_export_jobs",
        indexes = {
                @Index(name = "idx_report_job_status_retry", columnList = "status,next_attempt_at"),
                @Index(name = "idx_report_job_owner_created", columnList = "requester_user_id,created_at"),
                @Index(name = "idx_report_job_expiry", columnList = "status,expires_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_job_owner_idempotency",
                columnNames = {"requester_user_id", "idempotency_key_hash"}))
public class ReportExportJob extends BaseAuditEntity {

    @Id
    private UUID id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requester_college_id")
    private Long requesterCollegeId;

    @Column(name = "scope_college_id", nullable = false)
    private Long scopeCollegeId;

    @Column(name = "scope_department_id")
    private Long scopeDepartmentId;

    @Column(name = "requester_role", nullable = false, length = 40)
    private String requesterRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportExportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportExportStatus status = ReportExportStatus.QUEUED;

    @Column(name = "status_filter", length = 40)
    private String statusFilter;

    @Column(name = "idempotency_key_hash", nullable = false, length = 64)
    private String idempotencyKeyHash;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "result_key", length = 400)
    private String resultKey;

    @Column(name = "result_filename", length = 180)
    private String resultFilename;

    @Column(name = "result_content_type", length = 100)
    private String resultContentType;

    @Column(name = "result_size_bytes")
    private Long resultSizeBytes;

    @Column(name = "failure_reason", length = 250)
    private String failureReason;

    public ReportExportJob() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public Long getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(Long requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public Long getRequesterCollegeId() {
        return requesterCollegeId;
    }

    public void setRequesterCollegeId(Long requesterCollegeId) {
        this.requesterCollegeId = requesterCollegeId;
    }

    public Long getScopeCollegeId() {
        return scopeCollegeId;
    }

    public void setScopeCollegeId(Long scopeCollegeId) {
        this.scopeCollegeId = scopeCollegeId;
    }

    public Long getScopeDepartmentId() {
        return scopeDepartmentId;
    }

    public void setScopeDepartmentId(Long scopeDepartmentId) {
        this.scopeDepartmentId = scopeDepartmentId;
    }

    public String getRequesterRole() {
        return requesterRole;
    }

    public void setRequesterRole(String requesterRole) {
        this.requesterRole = requesterRole;
    }

    public ReportExportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportExportType reportType) {
        this.reportType = reportType;
    }

    public ReportExportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportExportStatus status) {
        this.status = status;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getIdempotencyKeyHash() {
        return idempotencyKeyHash;
    }

    public void setIdempotencyKeyHash(String idempotencyKeyHash) {
        this.idempotencyKeyHash = idempotencyKeyHash;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getResultKey() {
        return resultKey;
    }

    public void setResultKey(String resultKey) {
        this.resultKey = resultKey;
    }

    public String getResultFilename() {
        return resultFilename;
    }

    public void setResultFilename(String resultFilename) {
        this.resultFilename = resultFilename;
    }

    public String getResultContentType() {
        return resultContentType;
    }

    public void setResultContentType(String resultContentType) {
        this.resultContentType = resultContentType;
    }

    public Long getResultSizeBytes() {
        return resultSizeBytes;
    }

    public void setResultSizeBytes(Long resultSizeBytes) {
        this.resultSizeBytes = resultSizeBytes;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
