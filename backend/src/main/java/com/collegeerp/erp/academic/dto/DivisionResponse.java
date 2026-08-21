package com.collegeerp.erp.academic.dto;

import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.academic.enums.SectionStatus;

import java.time.LocalDateTime;

public record DivisionResponse(
        Long id,
        Long collegeId,
        String collegeName,
        String collegeCode,
        Long departmentId,
        String departmentName,
        String departmentCode,
        Long courseYearId,
        CourseYearName courseYearName,
        String courseYearDisplayName,
        String academicYear,
        String name,
        String code,
        Integer capacity,
        Long classTeacherId,
        String classTeacherName,
        String classTeacherEmail,
        SectionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
