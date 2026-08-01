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
import java.util.Objects;

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
                        admission.getCollege().getLogoUrl() == null ? null
                                : "/api/public/admissions/college/"
                                + admission.getCollege().getCode() + "/logo",
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
                        admission.getPincode(),
                        admission.getPhotoStorageName() != null,
                        admission.getPlaceOfBirth(),
                        admission.getMaritalStatus(),
                        admission.getAadhaarNumber(),
                        admission.getApaarId(),
                        admission.getNationality(),
                        admission.getReligion(),
                        admission.getCaste(),
                        admission.getPermanentPhone(),
                        admission.getPermanentEmail(),
                        admission.getCorrespondenceAddress(),
                        admission.getCorrespondenceCity(),
                        admission.getCorrespondencePincode(),
                        admission.getCorrespondenceState(),
                        admission.getCorrespondencePhone(),
                        admission.getCorrespondenceMobile(),
                        admission.getCorrespondenceEmail()
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
                        admission.getCourseYear() == null ? null
                                : (admission.getDepartment().getCode() + " "
                                        + admission.getCourseYear().getCode()).trim().toUpperCase(),
                        admission.getPreviousSchoolName(),
                        admission.getPreviousClassName(),
                        admission.getPreviousPercentage(),
                        admission.getAcademicRecords().stream()
                                .filter(Objects::nonNull)
                                .map(record -> new com.jadhavr.erp.admission.dto.AcademicRecordDto(
                                        record.getQualification(), record.getInstituteName(),
                                        record.getBoardUniversity(), record.getYearOfPassing(),
                                        record.getTotalMarks(), record.getObtainedMarks(),
                                        record.getMarksPercentage()))
                                .toList(),
                        admission.getQualifyingEntranceSeatNumber(),
                        admission.getQualifyingEntranceTotalScore(),
                        admission.getLastGraduationCollegeName(),
                        admission.getLastGraduationCollegeAddress()
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
