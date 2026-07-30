package com.jadhavr.erp.reports.dto;

import com.jadhavr.erp.admission.enums.AdmissionStatus;

public record AdmissionCsvRow(
        Long id,
        String reference,
        String admissionNumber,
        String studentName,
        String collegeName,
        String departmentName,
        AdmissionStatus status) {}
