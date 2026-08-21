package com.collegeerp.erp.staff.service;

import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.staff.dto.CreateStudentSectionStaffRequest;
import com.collegeerp.erp.staff.dto.CreateFeeSectionStaffRequest;
import com.collegeerp.erp.staff.dto.StaffResponse;
import com.collegeerp.erp.staff.dto.StaffDetailResponse;
import com.collegeerp.erp.staff.dto.CreateAcademicStaffRequest;
import com.collegeerp.erp.staff.dto.CreateStaffRequest;
import com.collegeerp.erp.staff.dto.UpdateStaffAssignmentRequest;
import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;

public interface StaffService {
    StaffResponse createStaff(CreateStaffRequest request);
    StaffResponse createStudentSectionStaff(CreateStudentSectionStaffRequest request);
    StaffResponse createFeeSectionStaff(CreateFeeSectionStaffRequest request);
    StaffResponse createAcademicStaff(CreateAcademicStaffRequest request, StaffType type);
    StaffResponse getStaffById(Long id);
    StaffDetailResponse getStaffDetails(Long id);
    StaffResponse updateStaffAssignment(Long id, UpdateStaffAssignmentRequest request);
    PageResponse<StaffResponse> searchStaff(
            String keyword,
            Long collegeId,
            Long departmentId,
            StaffType staffType,
            StaffStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
    StaffResponse activateStaff(Long id);
    StaffResponse deactivateStaff(Long id);
}
