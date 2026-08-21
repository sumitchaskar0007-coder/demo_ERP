package com.collegeerp.erp.staff.dto;

import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;

public record StaffResponse(
        Long id,
        Long userId,
        Long collegeId,
        String collegeName,
        String collegeCode,
        Long departmentId,
        String departmentName,
        String departmentCode,
        List<Long> departmentIds,
        List<String> departmentNames,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        StaffType staffType,
        Set<StaffType> staffTypes,
        StaffStatus status,
        Set<String> roles,
        LocalDate joiningDate,
        List<String> assignedClassTeacherDivisions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
