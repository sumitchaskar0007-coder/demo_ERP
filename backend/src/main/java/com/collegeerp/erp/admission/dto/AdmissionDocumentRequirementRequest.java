package com.collegeerp.erp.admission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdmissionDocumentRequirementRequest(
        @NotBlank @Size(min = 2, max = 120) String documentName,
        boolean required) {
}
