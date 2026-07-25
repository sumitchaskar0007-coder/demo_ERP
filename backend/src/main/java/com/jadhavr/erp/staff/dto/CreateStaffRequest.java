package com.jadhavr.erp.staff.dto;

import com.jadhavr.erp.staff.enums.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateStaffRequest(
        @NotBlank @Size(min = 2, max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank(message = "Phone number is required") @Size(max = 20) String phone,
        Long departmentId,
        StaffType staffType,
        Set<Long> departmentIds,
        Set<StaffType> staffTypes,
        LocalDate joiningDate
) {
}
