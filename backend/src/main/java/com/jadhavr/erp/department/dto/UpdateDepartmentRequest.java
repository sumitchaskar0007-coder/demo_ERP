package com.jadhavr.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(min = 2, max = 150,
                message = "Department name must be between 2 and 150 characters")
        String name,
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
