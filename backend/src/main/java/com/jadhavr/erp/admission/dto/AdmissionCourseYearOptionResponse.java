package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.academic.enums.CourseYearName;

public record AdmissionCourseYearOptionResponse(
        Long id,
        CourseYearName yearName,
        String displayName,
        String academicYear
) {}
