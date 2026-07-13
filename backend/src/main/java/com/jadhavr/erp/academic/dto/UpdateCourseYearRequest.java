package com.jadhavr.erp.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCourseYearRequest(
        @NotBlank @Size(min = 2, max = 150) String displayName,
        @NotBlank @Size(min = 1, max = 30) String code
) {
}
