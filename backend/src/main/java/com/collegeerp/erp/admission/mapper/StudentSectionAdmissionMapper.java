package com.collegeerp.erp.admission.mapper;

import com.collegeerp.erp.admission.dto.StudentSectionAdmissionResponse;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.enums.AdmissionDocumentType;
import com.collegeerp.erp.admission.repository.AdmissionDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class StudentSectionAdmissionMapper {
    private final AdmissionDocumentRepository documents;

    @Autowired
    public StudentSectionAdmissionMapper(AdmissionDocumentRepository documents) {
        this.documents = documents;
    }

    public StudentSectionAdmissionMapper() {
        this.documents = null;
    }

    public StudentSectionAdmissionResponse toResponse(AdmissionForm admission) {
        Set<String> uploadedDocuments = documents == null
                ? Set.of()
                : documents.findTypesByAdmissionId(admission.getId());
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
                admission.getCourseYear() == null ? null : admission.getCourseYear().getId(),
                admission.getCourseYear() == null ? null : admission.getCourseYear().getYearName(),
                admission.getCourseYear() == null ? null : admission.getCourseYear().getName(),
                admission.getStudentCategory(),
                admission.getCustomCategoryName(),
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
                available(uploadedDocuments, AdmissionDocumentType.TENTH_MARKSHEET.name(),
                        admission.getTenthMarksheetStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.TWELFTH_MARKSHEET.name(),
                        admission.getTwelfthMarksheetStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.GRADUATION_MARKSHEET.name(),
                        admission.getGraduationPgCertificateStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.TRANSFER_CERTIFICATE.name(),
                        admission.getLeavingCertificateStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.MIGRATION_CERTIFICATE.name(),
                        admission.getMigrationCertificateStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.GAP_CERTIFICATE.name(),
                        admission.getGapAffidavitStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.CASTE_CERTIFICATE.name(),
                        admission.getCasteCertificateStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.INCOME_CERTIFICATE.name(),
                        admission.getIncomeProofStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.NAME_CHANGE_CERTIFICATE.name(),
                        admission.getNameChangeCertificateStorageName()),
                available(uploadedDocuments, AdmissionDocumentType.AADHAAR_CARD.name(),
                        admission.getAadhaarCardStorageName()),
                admission.isPhotoVerified(),
                admission.isTenthMarksheetVerified(),
                admission.isTwelfthMarksheetVerified(),
                admission.isLeavingCertificateVerified(),
                admission.isAadhaarCardVerified(),
                admission.isGraduationPgCertificateVerified(),
                admission.isMigrationCertificateVerified(),
                admission.isGapAffidavitVerified(),
                admission.isCasteCertificateVerified(),
                admission.isIncomeProofVerified(),
                admission.isNameChangeCertificateVerified(),
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
                        .map(record -> new com.collegeerp.erp.admission.dto.AcademicRecordDto(
                                record.getQualification(), record.getInstituteName(),
                                record.getBoardUniversity(), record.getYearOfPassing(),
                                record.getTotalMarks(), record.getObtainedMarks(), gradingType(record),
                                record.getMarksPercentage(), record.getCgpa()))
                        .toList(),
                admission.getEntranceExams().stream()
                        .filter(Objects::nonNull)
                        .map(exam -> new com.collegeerp.erp.admission.dto.EntranceExamDto(
                                exam.getExamName(), exam.getResult()))
                        .toList(),
                admission.getQualifyingEntranceSeatNumber(),
                admission.getQualifyingEntranceTotalScore(),
                admission.getLastGraduationCollegeName(),
                admission.getLastGraduationCollegeAddress(),
                uploadedDocuments,
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

    private boolean available(Set<String> uploadedDocuments,
            String type, String legacyStorageName) {
        return legacyStorageName != null || uploadedDocuments.contains(type);
    }

    private com.collegeerp.erp.admission.enums.AcademicGradingType gradingType(
            com.collegeerp.erp.admission.entity.AdmissionAcademicRecord record) {
        return record.getGradingType() == null
                ? com.collegeerp.erp.admission.enums.AcademicGradingType.PERCENTAGE
                : record.getGradingType();
    }
}
