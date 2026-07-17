package com.jadhavr.erp.admission.mapper;

import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class StudentSectionAdmissionMapper {
    public StudentSectionAdmissionResponse toResponse(AdmissionForm admission) {
        return new StudentSectionAdmissionResponse(
                admission.getId(),
                admission.getAdmissionReferenceNumber(),
                admission.getStudent().getAdmissionNumber(),
                admission.getCollege().getId(),
                admission.getCollege().getName(),
                admission.getCollege().getCode(),
                admission.getDepartment().getId(),
                admission.getDepartment().getName(),
                admission.getDepartment().getCode(),
                admission.getAcademicYear(),
                admission.getStudentCategory(),
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
                admission.getParentName(),
                admission.getParentPhone(),
                admission.getParentEmail(),
                admission.getPreviousSchoolName(),
                admission.getPreviousClassName(),
                admission.getPreviousPercentage(),
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
                admission.getCorrespondenceEmail(),
                admission.getAcademicRecords().stream()
                        .filter(Objects::nonNull)
                        .map(record -> new com.jadhavr.erp.admission.dto.AcademicRecordDto(
                                record.getQualification(), record.getInstituteName(),
                                record.getBoardUniversity(), record.getYearOfPassing(),
                                record.getMarksPercentage()))
                        .toList(),
                admission.getQualifyingEntranceSeatNumber(),
                admission.getQualifyingEntranceTotalScore(),
                admission.getLastGraduationCollegeName(),
                admission.getLastGraduationCollegeAddress(),
                admission.getDetailsCompletedAt(),
                admission.getPrincipalApprovedAt(),
                admission.getStatus(),
                admission.getSource(),
                admission.getSubmittedAt(),
                admission.getStudentSectionVerifiedAt(),
                admission.getStudentSectionVerifiedBy() == null ? null : admission.getStudentSectionVerifiedBy().getFullName(),
                admission.getStudentSectionRemarks(),
                admission.getStudentSectionRejectedAt(),
                admission.getStudentSectionRejectedBy() == null ? null : admission.getStudentSectionRejectedBy().getFullName(),
                admission.getRejectionReason(),
                admission.getLastPrintedAt(),
                admission.getLastPrintedBy() == null ? null : admission.getLastPrintedBy().getFullName(),
                admission.getPrintCount(),
                admission.getCreatedAt(),
                admission.getUpdatedAt()
        );
    }
}
