package com.collegeerp.erp.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAdmissionRequest(
        @NotBlank @Size(min = 5, max = 500) String rejectionReason
) {
}
