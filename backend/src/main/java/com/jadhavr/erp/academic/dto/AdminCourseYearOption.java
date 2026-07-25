package com.jadhavr.erp.academic.dto;

import com.jadhavr.erp.academic.enums.CourseYearName;

public record AdminCourseYearOption(
        Long id,
        Long collegeId,
        Long departmentId,
        String displayName,
        CourseYearName yearName
) {}
