package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DetailedAdmissionRequest(
        @NotNull Long courseYearId,
        @NotBlank @Size(min = 2, max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotNull LocalDate dateOfBirth,
        @NotBlank @Size(max = 30) String gender,
        @NotBlank @Size(max = 120) String placeOfBirth,
        @NotBlank @Size(max = 30) String maritalStatus,
        @NotBlank @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must contain 12 digits") String aadhaarNumber,
        @Size(max = 30) String apaarId,
        @NotBlank @Size(max = 80) String nationality,
        @NotBlank @Size(max = 80) String religion,
        @NotBlank @Size(max = 100) String caste,
        @NotNull StudentCategory studentCategory,
        @Size(max = 80) String customCategoryName,
        @NotBlank @Size(max = 150) String parentName,
        @NotBlank @Size(max = 20) String parentPhone,
        @Email @Size(max = 150) String parentEmail,
        @NotBlank @Size(max = 250) String addressLine1,
        @Size(max = 250) String addressLine2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must contain 6 digits") String pincode,
        @NotBlank @Size(max = 100) String state,
        @Size(max = 20) String permanentPhone,
        @Email @Size(max = 150) String permanentEmail,
        @NotBlank @Size(max = 500) String correspondenceAddress,
        @NotBlank @Size(max = 100) String correspondenceCity,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must contain 6 digits") String correspondencePincode,
        @NotBlank @Size(max = 100) String correspondenceState,
        @Size(max = 20) String correspondencePhone,
        @Size(max = 20) String correspondenceMobile,
        @Email @Size(max = 150) String correspondenceEmail,
        @Valid @Size(max = 4) List<AcademicRecordDto> academicRecords,
        @Valid @Size(max = 10) List<EntranceExamDto> entranceExams,
        @Size(max = 80) String qualifyingEntranceSeatNumber,
        @DecimalMin("0.00") BigDecimal qualifyingEntranceTotalScore,
        @Size(max = 200) String lastGraduationCollegeName,
        @Size(max = 500) String lastGraduationCollegeAddress
) {}
