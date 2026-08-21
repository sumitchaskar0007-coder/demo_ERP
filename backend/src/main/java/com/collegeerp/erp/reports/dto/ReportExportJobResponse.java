package com.collegeerp.erp.reports.dto;

import com.collegeerp.erp.reports.entity.ReportExportJob;
import com.collegeerp.erp.reports.enums.ReportExportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportExportJobResponse(
        UUID id,
        String reportType,
        ReportExportStatus status,
        Long collegeId,
        Long departmentId,
        int attemptCount,
        int maxAttempts,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt) {

    public static ReportExportJobResponse from(ReportExportJob job) {
        return new ReportExportJobResponse(
                job.getId(),
                job.getReportType().pathValue(),
                job.getStatus(),
                job.getScopeCollegeId(),
                job.getScopeDepartmentId(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getFailureReason(),
                job.getCreatedAt(),
                job.getCompletedAt(),
                job.getExpiresAt());
    }
}
