package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.StudentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStudentCategoryRequest(
        @NotNull StudentCategory studentCategory,
        @Size(min = 2, max = 80) String customCategoryName,
        @NotBlank @Size(max = 150) String caste,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}
