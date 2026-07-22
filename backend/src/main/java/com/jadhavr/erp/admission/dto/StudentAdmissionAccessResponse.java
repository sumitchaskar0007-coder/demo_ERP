package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.admission.enums.AdmissionStatus;

public record StudentAdmissionAccessResponse(
        Long admissionId,
        AdmissionStatus status,
        boolean formCompleted,
        boolean editable,
        boolean pending,
        boolean accessGranted,
        String rejectionReason
) {}
