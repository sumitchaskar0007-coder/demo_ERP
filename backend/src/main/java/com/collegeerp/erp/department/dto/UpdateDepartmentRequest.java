package com.collegeerp.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record UpdateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(min = 2, max = 150,
                message = "Department name must be between 2 and 150 characters")
        String name,
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,
        @DecimalMin(value = "0.01", message = "Admission form fee must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Admission form fee must be a valid amount")
        BigDecimal admissionFormFee
) {
}
