package com.collegeerp.erp.admission.dto;

import com.collegeerp.erp.admission.enums.AdmissionStatus;

import java.time.LocalDateTime;

public record AdmissionPrintVerificationSection(
        AdmissionStatus status,
        LocalDateTime verifiedAt,
        String verifiedByName,
        String remarks
) {
}
