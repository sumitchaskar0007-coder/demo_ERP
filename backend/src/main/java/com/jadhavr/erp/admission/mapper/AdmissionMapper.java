package com.jadhavr.erp.admission.mapper;

import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import org.springframework.stereotype.Component;

@Component
public class AdmissionMapper {

    public AdmissionResponse toResponse(AdmissionForm admissionForm) {
        return new AdmissionResponse(
                admissionForm.getId(),
                admissionForm.getAdmissionReferenceNumber(),
                admissionForm.getStudent().getAdmissionNumber(),
                admissionForm.getCollege().getId(),
                admissionForm.getCollege().getName(),
                admissionForm.getCollege().getCode(),
                admissionForm.getDepartment().getId(),
                admissionForm.getDepartment().getName(),
                admissionForm.getDepartment().getCode(),
                admissionForm.getAcademicYear(),
                admissionForm.getStudentCategory(),
                admissionForm.getFullName(),
                admissionForm.getEmail(),
                admissionForm.getPhone(),
                admissionForm.getDateOfBirth(),
                admissionForm.getGender(),
                admissionForm.getAddressLine1(),
                admissionForm.getAddressLine2(),
                admissionForm.getCity(),
                admissionForm.getState(),
                admissionForm.getPincode(),
                admissionForm.getParentName(),
                admissionForm.getParentPhone(),
                admissionForm.getParentEmail(),
                admissionForm.getPreviousSchoolName(),
                admissionForm.getPreviousClassName(),
                admissionForm.getPreviousPercentage(),
                admissionForm.getStatus(),
                admissionForm.getSource(),
                admissionForm.getSubmittedAt(),
                admissionForm.getCreatedAt(),
                admissionForm.getUpdatedAt()
        );
    }
}
