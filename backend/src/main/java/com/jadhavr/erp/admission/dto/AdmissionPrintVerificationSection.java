package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.admission.enums.AdmissionStatus;

import java.time.LocalDateTime;

public record AdmissionPrintVerificationSection(
        AdmissionStatus status,
        LocalDateTime verifiedAt,
        String verifiedByName,
        String remarks
) {
}
