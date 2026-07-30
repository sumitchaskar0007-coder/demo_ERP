package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.admission.enums.AdmissionDocumentType;

import java.time.Instant;

public record AdmissionDocumentCompletionResponse(
        Long documentId,
        AdmissionDocumentType documentType,
        String originalFilename,
        String contentType,
        long fileSize,
        String sha256,
        Instant verifiedAt) {
}
