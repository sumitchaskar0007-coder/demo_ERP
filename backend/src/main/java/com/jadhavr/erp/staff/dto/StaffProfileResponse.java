package com.jadhavr.erp.staff.dto;

import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;

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
