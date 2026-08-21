package com.collegeerp.erp.audit.dto;

import java.time.*;
import java.util.*;

public final class BusinessActivityDtos {
    private BusinessActivityDtos() {}
    public record Summary(long todayActivities, long attendanceActivities, long admissionActivities,
            long feeActivities, long criticalChanges, long pendingApprovals) {}
    public record ActivityRow(Long id, LocalDateTime createdAt, Long userId, String user,
            String role, Long departmentId, String department, String module, String action,
            String title, String description, String status, String affectedRecords,
            String remarks, String previousValue, String newValue) {}
    public record TrendPoint(LocalDate date, long activities) {}
    public record ModuleAnalytics(String module, long today, long week, long month) {}
    public record DepartmentAnalytics(Long departmentId, String department, long activities,
            LocalDateTime lastActivity, String mostActiveUser, String status) {}
    public record Distribution(String label, long value) {}
    public record AlertItem(String key, String label, long count, String module, String action) {}
    public record TeacherEngagementSummary(long totalTeachers, long loggedInToday,
            long notLoggedInToday, long scheduledLecturesToday,
            long attendanceCompletedToday, long attendanceRemainingToday,
            long lowUsageTeachers) {}
    public record TeacherEngagementRow(Long staffId, Long userId, String teacher,
            String employeeCode, String department, String role, LocalDateTime lastLoginAt,
            int loginDaysLast7, boolean loggedInToday, long scheduledLectures,
            long attendanceSubmitted, long attendanceRemaining, long attendanceDraft,
            String usageStatus, String attendanceStatus) {}
    public record DashboardResponse(Summary summary, List<ActivityRow> timeline,
            List<ModuleAnalytics> modules, List<DepartmentAnalytics> departments,
            List<TrendPoint> trend, List<Distribution> actionDistribution,
            List<AlertItem> alerts, List<String> insights,
            TeacherEngagementSummary teacherSummary, List<TeacherEngagementRow> teachers,
            List<ActivityRow> rows,
            long totalElements, int totalPages, int page, int size) {}
}
