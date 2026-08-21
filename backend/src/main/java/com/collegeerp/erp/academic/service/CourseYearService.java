package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.dto.CreateCourseYearRequest;
import com.collegeerp.erp.academic.dto.CourseYearResponse;
import com.collegeerp.erp.academic.dto.UpdateCourseYearRequest;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.common.dto.PageResponse;

public interface CourseYearService {
    CourseYearResponse create(CreateCourseYearRequest request);
    PageResponse<CourseYearResponse> search(String keyword, Long departmentId, String academicYear,
            CourseYearName yearName, AcademicStatus status, int page, int size, String sortBy, String sortDir);
    CourseYearResponse get(Long id);
    CourseYearResponse update(Long id, UpdateCourseYearRequest request);
    CourseYearResponse setStatus(Long id, AcademicStatus status);
}
