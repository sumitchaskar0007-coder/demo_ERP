package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyAdmissionRequest(
        @NotNull StudentCategory studentCategory,
        @Size(max = 500) String remarks
) {
}
