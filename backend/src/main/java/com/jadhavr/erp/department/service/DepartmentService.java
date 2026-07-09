package com.jadhavr.erp.department.service;

import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.department.dto.CreateDepartmentRequest;
import com.jadhavr.erp.department.dto.DepartmentResponse;
import com.jadhavr.erp.department.dto.UpdateDepartmentRequest;
import com.jadhavr.erp.department.entity.DepartmentStatus;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse getDepartmentByCollegeIdAndCode(Long collegeId, String code);

    List<DepartmentResponse> getDepartmentsByCollege(Long collegeId);

    List<DepartmentResponse> getActiveDepartmentsByCollege(Long collegeId);

    DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request);

    DepartmentResponse activateDepartment(Long id);

    DepartmentResponse deactivateDepartment(Long id);

    PageResponse<DepartmentResponse> searchDepartments(
            String keyword,
            Long collegeId,
            DepartmentStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
}
