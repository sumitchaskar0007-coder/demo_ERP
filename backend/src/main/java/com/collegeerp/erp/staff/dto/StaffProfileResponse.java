package com.collegeerp.erp.staff.dto;

import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;

public record StaffProfileResponse(
        Long id,
        Long userId,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        StaffType staffType,
        StaffStatus status
) {
}
