package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.CreateCourseYearRequest;
import com.jadhavr.erp.academic.dto.CourseYearResponse;
import com.jadhavr.erp.academic.dto.UpdateCourseYearRequest;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.common.dto.PageResponse;

public interface CourseYearService {
    CourseYearResponse create(CreateCourseYearRequest request);
    PageResponse<CourseYearResponse> search(String keyword, Long departmentId, String academicYear,
            CourseYearName yearName, AcademicStatus status, int page, int size, String sortBy, String sortDir);
    CourseYearResponse get(Long id);
    CourseYearResponse update(Long id, UpdateCourseYearRequest request);
    CourseYearResponse setStatus(Long id, AcademicStatus status);
}
