package com.collegeerp.erp.academic.dto;

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
    public record WorkspaceResponse(Long departmentId,String department,Summary summary,List<DivisionCard> divisions,List<StudentRow> students,long totalStudents,int page,int totalPages,List<TeacherRow> teachers,List<TeacherRow> eligibleClassTeachers,List<SubjectRow> subjects,List<TimetableReviewRow> timetables,List<ActivityItem> recentActivity) {}
 public record BulkAllocationRequest(
  @NotNull @Positive Long sectionId,
  @NotEmpty @Size(max=500) List<@NotNull @Positive Long> studentIds) {}
 public record AutomaticAllocationRequest(
  @NotNull @Positive Long courseYearId,
  @Size(max=500) List<@NotNull @Positive Long> studentIds) {}
 public record TransferRequest(
  @NotNull @Positive Long targetSectionId,
  @NotEmpty @Size(max=500) List<@NotNull @Positive Long> studentIds) {}
 public record SubjectAllocationRequest(
  @NotNull @Positive Long teacherId,
  @NotEmpty @Size(max=100) List<@NotNull @Positive Long> divisionIds) {}
    public record ClassTeacherRequest(@NotNull @Positive Long teacherId) {}
    public record TimetableReviewRequest(
     @NotBlank @Pattern(regexp="^(APPROVE|REJECT|REQUEST_CHANGES)$") String action,
     @Size(max=1000) String comment) {}
}
