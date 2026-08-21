package com.collegeerp.erp.academic.dto;

import com.collegeerp.erp.academic.enums.AcademicYearStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class GlobalAcademicYearDtos {
    private GlobalAcademicYearDtos() {}

    public record SaveRequest(
            @NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Use academic year format YYYY-YYYY") String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate) {}

    public record View(Long id, String name, LocalDate startDate, LocalDate endDate,
            AcademicYearStatus status, LocalDateTime activatedAt, int attachedColleges) {}
}
