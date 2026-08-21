package com.collegeerp.erp.admission.dto;

import java.time.Instant;

public record AdmissionDocumentDownloadUrlResponse(
        String downloadUrl,
        Instant expiresAt,
        String originalFilename,
        String contentType,
        long fileSize,
        String sha256) {
}
