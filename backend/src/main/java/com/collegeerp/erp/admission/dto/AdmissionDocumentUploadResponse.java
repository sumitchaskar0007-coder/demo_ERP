package com.collegeerp.erp.admission.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdmissionDocumentUploadResponse(
        UUID uploadId,
        String uploadUrl,
        Map<String, List<String>> requiredHeaders,
        Instant uploadUrlExpiresAt,
        Instant completionDeadline) {
}
