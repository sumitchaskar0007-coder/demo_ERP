package com.collegeerp.erp.staff.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record StaffDetailResponse(
        StaffResponse staff,
        List<ClassAssignment> classAssignments,
        List<SubjectAssignment> subjectAssignments,
        AttendanceSummary attendanceSummary,
        List<AttendanceSessionItem> recentAttendance
) {
    public record ClassAssignment(
            Long sectionId,
            String departmentName,
            String className,
            String sectionName,
            String sectionCode,
            String academicYear,
            Integer capacity
    ) {
    }

    public record SubjectAssignment(
            Long subjectId,
            String subjectCode,
            String subjectName,
            String className,
            String academicYear,
            List<String> divisions
    ) {
    }

    public record AttendanceSummary(
            long totalSessions,
            long submittedSessions,
            long draftSessions,
            long studentsMarked,
            long present,
            long absent,
            long late,
            long leave
    ) {
    }

    public record AttendanceSessionItem(
            Long sessionId,
            LocalDate attendanceDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer lectureNumber,
            String subjectName,
            String className,
            String sectionName,
            String status,
            long studentsMarked,
            long present,
            long absent,
            long late,
            long leave
    ) {
    }
}
