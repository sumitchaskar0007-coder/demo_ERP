package com.jadhavr.erp.staff.service;

import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.staff.dto.CreateStudentSectionStaffRequest;
import com.jadhavr.erp.staff.dto.CreateFeeSectionStaffRequest;
import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.dto.StaffDetailResponse;
import com.jadhavr.erp.staff.dto.CreateAcademicStaffRequest;
import com.jadhavr.erp.staff.dto.CreateStaffRequest;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;

public interface StaffService {
    StaffResponse createStaff(CreateStaffRequest request);
    StaffResponse createStudentSectionStaff(CreateStudentSectionStaffRequest request);
    StaffResponse createFeeSectionStaff(CreateFeeSectionStaffRequest request);
    StaffResponse createAcademicStaff(CreateAcademicStaffRequest request, StaffType type);
    StaffResponse getStaffById(Long id);
    StaffDetailResponse getStaffDetails(Long id);
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
