package com.jadhavr.erp.reports.dto;

import java.time.*;
import java.util.*;

public final class AdmissionAnalyticsDtos {
    private AdmissionAnalyticsDtos() {}

    public record Summary(long totalApplications, long approvedAdmissions, long pendingReviews,
            long rejectedApplications, long todayApplications, double successRate,
            long documentsPending, long feePending, double averageProcessingDays) {}
    public record FunnelStage(String key, String label, long count) {}
    public record TrendPoint(LocalDate date, long applications, long approved, long rejected) {}
    public record GroupAnalytics(String key, String label, String parentKey, long applications,
            long approved, long pending, long rejected, double admissionRate, String classTeacher) {}
    public record TimelineItem(String stage, String status, LocalDateTime date,
            String responsibleUser, String remarks) {}
    public record AdmissionRow(Long id, String admissionReferenceNumber, String admissionNumber,
            String studentName, String gender, LocalDate dateOfBirth, String email, String phone,
            String guardianName, String guardianPhone, String address, Long collegeId, String college,
            Long departmentId, String department, String departmentCode, String academicYear,
            String year, Long divisionId, String division, String classTeacher, String status,
            LocalDateTime submittedAt, LocalDateTime approvedAt, boolean documentsUploaded,
            boolean feePaid, boolean feePending, String remarks, int processingDays,
            List<TimelineItem> timeline) {}
    public record AnalyticsResponse(Summary summary, List<FunnelStage> funnel,
            List<TrendPoint> trend, List<GroupAnalytics> departments, List<GroupAnalytics> years,
            List<GroupAnalytics> divisions, List<AdmissionRow> rows, long totalElements,
            int totalPages, int page, int size) {}
}
