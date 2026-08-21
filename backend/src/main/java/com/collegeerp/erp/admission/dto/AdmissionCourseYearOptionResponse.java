package com.collegeerp.erp.admission.dto;

import com.collegeerp.erp.academic.enums.CourseYearName;

public record AdmissionCourseYearOptionResponse(
        Long id,
        CourseYearName yearName,
        String displayName,
        String academicYear
) {}
