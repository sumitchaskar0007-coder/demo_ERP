package com.jadhavr.erp.admission.mapper;

import com.jadhavr.erp.admission.dto.AdmissionPrintAcademicSection;
import com.jadhavr.erp.admission.dto.AdmissionPrintCollegeSection;
import com.jadhavr.erp.admission.dto.AdmissionPrintParentSection;
import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionPrintStudentSection;
import com.jadhavr.erp.admission.dto.AdmissionPrintVerificationSection;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AdmissionPrintMapper {
    public AdmissionPrintResponse toResponse(AdmissionForm admission) {
        return new AdmissionPrintResponse(
                admission.getId(),
                admission.getAdmissionReferenceNumber(),
                admission.getStudent().getAdmissionNumber(),
                LocalDateTime.now(),
                admission.getPrintCount(),
                new AdmissionPrintCollegeSection(
                        admission.getCollege().getName(),
                        admission.getCollege().getCode(),
                        admission.getCollege().getLogoUrl(),
                        admission.getCollege().getAddress(),
                        admission.getCollege().getCity(),
                        admission.getCollege().getState(),
                        admission.getCollege().getContactEmail(),
                        admission.getCollege().getContactPhone()
                ),
                new AdmissionPrintStudentSection(
                        admission.getFullName(),
                        admission.getEmail(),
                        admission.getPhone(),
                        admission.getDateOfBirth(),
                        admission.getGender(),
                        admission.getAddressLine1(),
                        admission.getAddressLine2(),
                        admission.getCity(),
                        admission.getState(),
                        admission.getPincode()
                ),
                new AdmissionPrintParentSection(
                        admission.getParentName(),
                        admission.getParentPhone(),
                        admission.getParentEmail()
                ),
                new AdmissionPrintAcademicSection(
                        admission.getAcademicYear(),
                        admission.getDepartment().getName(),
                        admission.getDepartment().getCode(),
                        admission.getPreviousSchoolName(),
                        admission.getPreviousClassName(),
                        admission.getPreviousPercentage()
                ),
                new AdmissionPrintVerificationSection(
                        admission.getStatus(),
                        admission.getStudentSectionVerifiedAt(),
                        admission.getStudentSectionVerifiedBy() == null ? null : admission.getStudentSectionVerifiedBy().getFullName(),
                        admission.getStudentSectionRemarks()
                ),
                List.of(
                        "I hereby declare that the information provided above is true and correct.",
                        "I understand that admission is subject to verification and approval.",
                        "I agree to follow the rules and regulations of the institution."
                ),
                List.of(
                        "Student Signature",
                        "Parent/Guardian Signature",
                        "Student Section Signature",
                        "Principal Signature"
                )
        );
    }
}
