package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.admission.enums.AdmissionStatus;

public record SubmitAdmissionResponse(
        String admissionReferenceNumber,
        String admissionNumber,
        AdmissionStatus status,
        Long studentUserId,
        Long studentProfileId,
        String collegeName,
        String collegeCode,
        String departmentName,
        String departmentCode,
        String studentName,
        String email,
        String loginUrl,
        String message
) {
}
