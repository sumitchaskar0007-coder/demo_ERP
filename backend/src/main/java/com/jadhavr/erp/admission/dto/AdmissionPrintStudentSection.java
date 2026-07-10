package com.jadhavr.erp.admission.dto;

import java.time.LocalDate;

public record AdmissionPrintStudentSection(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode
) {
}
