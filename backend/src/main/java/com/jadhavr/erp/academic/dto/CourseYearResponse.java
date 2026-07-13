package com.jadhavr.erp.academic.dto;

import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;

import java.time.LocalDateTime;

public record CourseYearResponse(
        Long id,
        Long collegeId,
        String collegeName,
        String collegeCode,
        Long departmentId,
        String departmentName,
        String departmentCode,
        String academicYear,
        CourseYearName yearName,
        String displayName,
        String code,
        AcademicStatus status,
        long divisionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
