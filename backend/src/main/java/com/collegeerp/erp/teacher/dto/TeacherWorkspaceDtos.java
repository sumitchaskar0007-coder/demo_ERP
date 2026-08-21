package com.collegeerp.erp.teacher.dto;

import java.time.*;
import java.util.List;
import jakarta.validation.constraints.*;

public final class TeacherWorkspaceDtos {
    private TeacherWorkspaceDtos() {}
    public record Kpis(long todayLectures,long pendingAttendance,long completedAttendance,long subjects,long divisions,Long classStrength,double averageAttendance){}
    public record ClassSummary(Long divisionId,String className,String department,String courseYear,String division,String academicYear,long totalStudents,long maleStudents,long femaleStudents,double averageAttendance){}
    public record StudentRow(Long id,String rollNumber,String name,String admissionNumber,String prn,String division,String gender,double attendancePercentage,String email,String status,String attentionIssue){}
    public record StudentIdentifierRequest(@NotBlank @Size(max=60) String prn,@NotBlank @Size(max=60) String rollNumber){}
    public record StudentPage(List<StudentRow> content,long totalElements,int page,int totalPages){}
    public record AttendanceHistoryRow(LocalDate date,String time,String subject,String division,String status,String remarks){}
    public record AttendanceMetrics(double today,double weekly,double monthly,double overall,long above90,long between75And90,long below75,long below60){}
    public record TrendPoint(String label,double percentage){}
    public record Workload(long totalSubjects,long totalDivisions,long weeklyLectures,long todayLectures,long pendingAttendanceSessions,List<WorkloadPoint> weekly,List<WorkloadPoint> bySubject){}
    public record WorkloadPoint(String label,long value){}
    public record ScheduleRow(Long timetableEntryId,String time,String subject,String division,String lectureType,String state,Long attendanceSessionId,boolean canTakeAttendance,boolean substituted,String originalTeacher){}
    public record NoticeRow(Long id,String title,String createdBy,LocalDateTime date,String priority,boolean unread,String message){}
    public record NotificationRow(Long id,String type,String message,LocalDateTime createdAt,boolean unread){}
    public record ActivityRow(String type,String message,LocalDateTime occurredAt){}
    public record DivisionInsight(Long divisionId,String division,long totalStudents,double averageAttendance,long lowAttendanceStudents,List<TrendPoint> trend){}
    public record Workspace(String teacherName,String employeeCode,boolean classTeacher,boolean subjectTeacher,Kpis kpis,List<ClassSummary> classes,StudentPage students,AttendanceMetrics attendance,List<TrendPoint> dailyTrend,List<TrendPoint> weeklyTrend,List<TrendPoint> monthlyTrend,List<StudentRow> attention,Workload workload,List<ScheduleRow> todaySchedule,List<NoticeRow> notices,List<NotificationRow> notifications,List<ActivityRow> recentActivities,List<DivisionInsight> divisionInsights){}
}
