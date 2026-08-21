package com.collegeerp.erp.academic.dto;

import com.collegeerp.erp.academic.enums.CourseYearName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseYearRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(max = 20) String academicYear,
        @NotNull CourseYearName yearName,
        @NotBlank @Size(min = 2, max = 150) String displayName,
        @NotBlank @Size(min = 1, max = 30) String code
) {
}
