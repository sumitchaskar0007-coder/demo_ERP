package com.jadhavr.erp.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateDivisionRequest(
        @NotNull Long courseYearId,
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Size(min = 1, max = 20) String code,
        @NotNull @Positive Integer capacity
) {
}
