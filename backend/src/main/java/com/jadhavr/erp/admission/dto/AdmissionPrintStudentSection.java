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
        String pincode,
        boolean hasPhoto,
        String placeOfBirth,
        String maritalStatus,
        String aadhaarNumber,
        String apaarId,
        String nationality,
        String religion,
        String caste,
        String permanentPhone,
        String permanentEmail,
        String correspondenceAddress,
        String correspondenceCity,
        String correspondencePincode,
        String correspondenceState,
        String correspondencePhone,
        String correspondenceMobile,
        String correspondenceEmail
) {
}
