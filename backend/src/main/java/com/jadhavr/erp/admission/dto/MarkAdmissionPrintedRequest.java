package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.Size;

public record MarkAdmissionPrintedRequest(
        @Size(max = 500) String remarks
) {
}
