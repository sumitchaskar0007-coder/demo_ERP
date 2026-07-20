package com.jadhavr.erp.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class WeeklyAttendanceDtos {
    private WeeklyAttendanceDtos() {}

    public record LectureResponse(Long lectureId, Long sessionId, String sessionStatus, LocalDate date,
            String period, int lectureNumber, LocalTime startTime, LocalTime endTime, Long subjectId,
            String subject, String subjectCode, Long departmentId, String department, Long divisionId,
            String year, String division, String lectureType, String room, boolean active, boolean canMark) {}
    public record StudentRow(Long recordId, Long studentId, String rollNumber, String admissionNumber,
            String studentName, String photoUrl, String status, String remarks) {}
    public record RosterResponse(LectureResponse lecture, List<StudentRow> students, int totalStudents,
            Map<String, Long> counts, boolean editable) {}
    public record MarkItem(@NotNull Long studentId, @NotBlank String status, @Size(max=500) String remarks) {}
    public record MarkRequest(@NotNull Long lectureId, @NotEmpty List<@Valid MarkItem> records, boolean submit) {}
    public record UpdateRequest(@NotEmpty List<@Valid MarkItem> records, boolean submit) {}
    public record SessionSummary(Long id, LocalDate date, String time, Long subjectId, String subject,
            Long departmentId, String department, Long divisionId, String year, String division,
            String teacher, String status, int total, long present, long absent, long late, long leave,
            double percentage) {}
    public record SubjectSummary(Long subjectId, String subject, int total, int attended, int absent,
            int late, int leave, double percentage, String indicator) {}
    public record MonthSummary(String month, int total, int attended, double percentage) {}
    public record StudentAttendanceResponse(Long studentId, String studentName, String rollNumber,
            double overallPercentage, String indicator, List<SubjectSummary> subjects,
            List<MonthSummary> monthly, List<StudentHistoryRow> history) {}
    public record StudentHistoryRow(LocalDate date, String time, String subject, String teacher,
            String status, String remarks) {}
    public record TrendPoint(LocalDate date, int total, int attended, double percentage) {}
    public record OperationalSummary(Long id, String name, int lectures, int submitted, int pending) {}
    public record StudentAnalyticsRow(Long studentId, String admissionNumber, String rollNumber,
            String studentName, String gender, String photoUrl, String guardianName, String mobile,
            Long departmentId, String department, String academicYear, String year,
            Long divisionId, String division, String classTeacher, int total, long present,
            long absent, long late, long leave, double percentage, String indicator,
            List<SubjectSummary> subjects, List<MonthSummary> monthly, List<StudentHistoryRow> history) {}
    public record ReportResponse(LocalDate from, LocalDate to, int sessions, int totalMarks,
            long present, long absent, long late, long leave, double percentage,
            int totalStudents, int uniquePresentToday, int uniqueAbsentToday,
            int todayLectures, int submittedToday, int pendingToday, int departments,
            int divisions, int below75, int below50, List<TrendPoint> trend,
            List<OperationalSummary> departmentOperations, List<OperationalSummary> divisionOperations,
            List<OperationalSummary> teacherOperations, List<StudentAnalyticsRow> students, List<SessionSummary> rows,
            List<SubjectSummary> subjects, List<MonthSummary> monthly) {}
}
