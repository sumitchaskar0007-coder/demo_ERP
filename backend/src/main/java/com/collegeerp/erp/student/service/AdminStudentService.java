package com.collegeerp.erp.student.service;

import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.student.dto.StudentProfileResponse;
import com.collegeerp.erp.student.enums.StudentStatus;

public interface AdminStudentService {
    StudentProfileResponse getStudentById(Long id);
    PageResponse<StudentProfileResponse> searchStudents(
            String keyword,
            Long collegeId,
            Long departmentId,
            StudentStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
}
