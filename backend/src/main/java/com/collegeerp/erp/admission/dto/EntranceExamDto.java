package com.collegeerp.erp.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntranceExamDto(
        @NotBlank @Size(max = 120) String examName,
        @NotBlank @Size(max = 100) String result
) {}
