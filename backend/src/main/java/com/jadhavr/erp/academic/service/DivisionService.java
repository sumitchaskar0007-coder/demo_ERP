package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.dto.AssignClassTeacherRequest;
import com.jadhavr.erp.academic.dto.CreateDivisionRequest;
import com.jadhavr.erp.academic.dto.DivisionResponse;
import com.jadhavr.erp.academic.dto.UpdateDivisionRequest;
import com.jadhavr.erp.academic.enums.SectionStatus;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.staff.dto.StaffResponse;

import java.util.List;

public interface DivisionService {
    DivisionResponse create(CreateDivisionRequest request);
    PageResponse<DivisionResponse> search(String keyword, Long departmentId, Long courseYearId,
            String academicYear, SectionStatus status, int page, int size);
    DivisionResponse get(Long id);
    DivisionResponse update(Long id, UpdateDivisionRequest request);
    DivisionResponse setStatus(Long id, SectionStatus status);
    DivisionResponse assignClassTeacher(Long id, AssignClassTeacherRequest request);
    DivisionResponse removeClassTeacher(Long id);
    List<StaffResponse> eligibleClassTeachers(Long id);
}
