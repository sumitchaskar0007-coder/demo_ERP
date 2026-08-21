package com.collegeerp.erp.academic.dto;

import com.collegeerp.erp.academic.enums.CourseYearName;

public record AdminCourseYearOption(
        Long id,
        Long collegeId,
        Long departmentId,
        String displayName,
        CourseYearName yearName
) {}
