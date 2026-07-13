package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.admission.enums.AdmissionSource;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdmissionResponse(
        Long id,
        String admissionReferenceNumber,
        String admissionNumber,
        Long collegeId,
        String collegeName,
        String collegeCode,
        Long departmentId,
        String departmentName,
        String departmentCode,
        String academicYear,
        StudentCategory studentCategory,
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
        String parentName,
        String parentPhone,
        String parentEmail,
        String previousSchoolName,
        String previousClassName,
        BigDecimal previousPercentage,
        AdmissionStatus status,
        AdmissionSource source,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
