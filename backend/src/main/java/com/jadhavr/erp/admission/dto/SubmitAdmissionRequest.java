package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitAdmissionRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(min = 2, max = 80) String firstName,
        @Size(max = 80) String middleName,
        @NotBlank @Size(min = 2, max = 80) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotNull LocalDate dateOfBirth,
        @NotBlank @Size(max = 30) String gender,
        @Size(max = 250) String addressLine1,
        @Size(max = 250) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 10) String pincode,
        @NotBlank @Size(min = 2, max = 150) String parentName,
        @NotBlank @Size(max = 20) String parentPhone,
        @Email @Size(max = 150) String parentEmail,
        @Size(max = 200) String previousSchoolName,
        @Size(max = 100) String previousClassName,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal previousPercentage
) {
}
