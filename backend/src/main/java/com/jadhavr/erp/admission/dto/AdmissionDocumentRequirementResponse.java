package com.jadhavr.erp.admission.dto;

public record AdmissionDocumentRequirementResponse(
        Long id,
        String documentKey,
        String documentName,
        boolean required,
        boolean active,
        int displayOrder) {
}
