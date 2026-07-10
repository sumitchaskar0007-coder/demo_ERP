package com.jadhavr.erp.admission.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdmissionPrintResponse(
        Long admissionId,
        String admissionReferenceNumber,
        String admissionNumber,
        LocalDateTime generatedAt,
        Integer printCount,
        AdmissionPrintCollegeSection college,
        AdmissionPrintStudentSection student,
        AdmissionPrintParentSection parent,
        AdmissionPrintAcademicSection academic,
        AdmissionPrintVerificationSection verification,
        List<String> declarations,
        List<String> signatureLabels
) {
}
