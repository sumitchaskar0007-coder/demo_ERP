package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.Size;

public record VerifyAdmissionRequest(
        @Size(max = 500) String remarks
) {
}
