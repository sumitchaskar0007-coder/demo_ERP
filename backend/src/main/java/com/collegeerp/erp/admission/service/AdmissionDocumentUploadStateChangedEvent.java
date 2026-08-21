package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.enums.AdmissionDocumentUploadStatus;

import java.util.UUID;

/**
 * Post-commit hook for antivirus/quarantine processors and storage lifecycle
 * automation. Event consumers must not make an object public.
 */
public record AdmissionDocumentUploadStateChangedEvent(
        UUID uploadId,
        Long collegeId,
        String storageKey,
        AdmissionDocumentUploadStatus status,
        String reasonCode) {
}
