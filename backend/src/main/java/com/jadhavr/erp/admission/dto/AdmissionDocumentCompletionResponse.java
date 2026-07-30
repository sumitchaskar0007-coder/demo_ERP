package com.jadhavr.erp.admission.dto;


import java.time.Instant;

public record AdmissionDocumentCompletionResponse(
        Long documentId,
        String documentType,
        String originalFilename,
        String contentType,
        long fileSize,
        String sha256,
        Instant verifiedAt) {
}
