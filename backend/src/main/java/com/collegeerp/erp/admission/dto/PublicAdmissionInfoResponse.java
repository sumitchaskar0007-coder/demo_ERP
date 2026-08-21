package com.collegeerp.erp.admission.dto;

import java.util.List;

public record PublicAdmissionInfoResponse(
        Long collegeId,
        String collegeName,
        String collegeCode,
        String logoUrl,
        String contactEmail,
        String contactPhone,
        String address,
        String city,
        String state,
        String academicYear,
        List<AdmissionDepartmentOptionResponse> departments
) {
}
