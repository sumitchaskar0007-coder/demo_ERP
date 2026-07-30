package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdmissionDocumentCompleteRequest(@NotNull UUID uploadId) {
}
