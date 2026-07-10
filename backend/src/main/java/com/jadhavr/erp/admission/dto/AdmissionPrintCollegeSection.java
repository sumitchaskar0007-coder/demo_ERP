package com.jadhavr.erp.admission.dto;

public record AdmissionPrintCollegeSection(
        String collegeName,
        String collegeCode,
        String logoUrl,
        String address,
        String city,
        String state,
        String contactEmail,
        String contactPhone
) {
}
