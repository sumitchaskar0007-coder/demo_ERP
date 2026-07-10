package com.jadhavr.erp.student.service;

import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.student.dto.StudentProfileResponse;
import com.jadhavr.erp.student.enums.StudentStatus;

public interface AdminStudentService {
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
