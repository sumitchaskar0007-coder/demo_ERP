package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.jadhavr.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitAdmissionRequest(
        @NotNull Long departmentId,
        @NotNull StudentCategory studentCategory,
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
        @Size(max = 150) String parentName,
        @Size(max = 20) String parentPhone,
        @Email @Size(max = 150) String parentEmail,
        @Size(max = 200) String previousSchoolName,
        @Size(max = 100) String previousClassName,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal previousPercentage,
        @Size(max = 80) String customCategoryName
) {
    public SubmitAdmissionRequest(Long departmentId, StudentCategory studentCategory, String firstName,
            String middleName, String lastName, String email, String phone, LocalDate dateOfBirth,
            String gender, String addressLine1, String addressLine2, String city, String state,
            String pincode, String parentName, String parentPhone, String parentEmail,
            String previousSchoolName, String previousClassName, BigDecimal previousPercentage) {
        this(departmentId, studentCategory, firstName, middleName, lastName, email, phone,
                dateOfBirth, gender, addressLine1, addressLine2, city, state, pincode,
                parentName, parentPhone, parentEmail, previousSchoolName, previousClassName,
                previousPercentage, null);
    }
}
