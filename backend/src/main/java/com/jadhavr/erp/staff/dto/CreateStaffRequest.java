package com.jadhavr.erp.staff.dto;

import com.jadhavr.erp.staff.enums.StaffType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStaffRequest(
        @NotBlank @Size(min = 2, max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "Password must contain uppercase, lowercase, number and special character")
        String password,
        Long departmentId,
        @NotNull StaffType staffType,
        LocalDate joiningDate
) {
}
