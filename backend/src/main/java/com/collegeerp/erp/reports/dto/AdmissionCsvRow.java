package com.collegeerp.erp.reports.dto;

import com.collegeerp.erp.admission.enums.AdmissionStatus;

public record AdmissionCsvRow(
        Long id,
        String reference,
        String admissionNumber,
        String studentName,
        String collegeName,
        String departmentName,
        AdmissionStatus status) {}
