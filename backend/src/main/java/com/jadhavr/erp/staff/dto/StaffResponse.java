package com.jadhavr.erp.staff.dto;

import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;

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
        String employeeCode,
        String fullName,
        String email,
        String phone,
        StaffType staffType,
        StaffStatus status,
        Set<String> roles,
        LocalDate joiningDate,
        List<String> assignedClassTeacherDivisions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
