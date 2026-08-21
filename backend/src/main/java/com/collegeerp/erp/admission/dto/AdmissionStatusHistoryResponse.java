package com.collegeerp.erp.admission.dto;

import com.collegeerp.erp.admission.enums.AdmissionAction;
import com.collegeerp.erp.admission.enums.AdmissionStatus;

import java.time.LocalDateTime;

public record AdmissionStatusHistoryResponse(
        Long id,
        Long admissionId,
        String admissionReferenceNumber,
        AdmissionStatus oldStatus,
        AdmissionStatus newStatus,
        AdmissionAction action,
        String remarks,
        Long changedByUserId,
        String changedByName,
        LocalDateTime createdAt
) {
}
