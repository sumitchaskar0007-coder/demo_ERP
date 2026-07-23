package com.jadhavr.erp.academic.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class HodModuleDtos {
    private HodModuleDtos() {}
    public record Summary(long totalStudents,long totalTeachers,long totalDivisions,long totalSubjects,long classesRunningToday,double averageAttendance,long pendingTasks) {}
    public record DivisionCard(Long id,Long courseYearId,String courseYear,String academicYear,String name,String code,int capacity,long allocated,String classTeacher) {}
    public record StudentRow(Long id,String photoUrl,String name,String prn,String admissionNumber,String gender,String admissionStatus,Long enrollmentId,Long courseYearId,String courseYear,String academicYear,Long divisionId,String division,String rollNumber,String allocationStatus) {}
    public record TeacherRow(Long id,String employeeCode,String name,String email,String staffType,long subjects,long divisions,long weeklyLectures,long remainingCapacity,String loadStatus) {}
    public record SubjectRow(Long id,String code,String name,Long courseYearId,String courseYear,String academicYear,Integer weeklyLectures,List<Long> teacherIds,List<String> teacherNames,List<Long> divisionIds) {}
    public record TimetableReviewRow(Long id,Long divisionId,String division,String courseYear,String academicYear,String reviewStatus,String comment,LocalDateTime submittedAt,LocalDateTime reviewedAt,long lectures) {}
    public record ActivityItem(String type,String message,LocalDateTime occurredAt) {}
    public record WorkspaceResponse(Long departmentId,String department,Summary summary,List<DivisionCard> divisions,List<StudentRow> students,long totalStudents,int page,int totalPages,List<TeacherRow> teachers,List<SubjectRow> subjects,List<TimetableReviewRow> timetables,List<ActivityItem> recentActivity) {}
    public record BulkAllocationRequest(@NotNull Long sectionId,@NotEmpty List<@NotNull Long> studentIds) {}
    public record AutomaticAllocationRequest(@NotNull Long courseYearId,List<Long> studentIds) {}
    public record TransferRequest(@NotNull Long targetSectionId,@NotEmpty List<@NotNull Long> studentIds) {}
    public record RollPreviewRequest(@NotNull Long sectionId,@NotBlank String strategy) {}
    public record RollAssignment(@NotNull Long studentId,@NotBlank @Size(max=60) String rollNumber) {}
    public record RollConfirmRequest(@NotNull Long sectionId,@NotEmpty List<RollAssignment> assignments,boolean overwrite) {}
    public record RollPreview(Long studentId,String studentName,String admissionNumber,String currentRollNumber,String proposedRollNumber) {}
    public record SubjectAllocationRequest(@NotNull Long teacherId,@NotEmpty List<@NotNull Long> divisionIds) {}
    public record ClassTeacherRequest(@NotNull Long teacherId) {}
    public record TimetableReviewRequest(@NotBlank String action,@Size(max=1000) String comment) {}
}
