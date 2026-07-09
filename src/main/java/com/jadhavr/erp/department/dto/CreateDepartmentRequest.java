package com.jadhavr.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotNull(message = "College ID is required") Long collegeId,
        @NotBlank(message = "Department name is required")
        @Size(min = 2, max = 150,
                message = "Department name must be between 2 and 150 characters")
        String name,
        @NotBlank(message = "Department code is required")
        @Size(min = 2, max = 30,
                message = "Department code must be between 2 and 30 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$",
                message = "Department code may contain only letters, numbers, underscores and hyphens")
        String code,
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
