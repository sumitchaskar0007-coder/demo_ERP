package com.collegeerp.erp.admission.dto;

import com.collegeerp.erp.admission.enums.AdmissionStatus;

public record StudentAdmissionAccessResponse(
        Long admissionId,
        AdmissionStatus status,
        boolean formCompleted,
        boolean editable,
        boolean pending,
        boolean accessGranted,
        String rejectionReason
) {}
