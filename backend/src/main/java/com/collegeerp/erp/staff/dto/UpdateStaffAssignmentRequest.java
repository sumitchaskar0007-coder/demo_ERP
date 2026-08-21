package com.collegeerp.erp.staff.dto;

import com.collegeerp.erp.staff.enums.StaffType;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateStaffAssignmentRequest(
        @NotEmpty(message = "Select at least one staff role") Set<StaffType> staffTypes,
        Set<Long> departmentIds
) {
}
