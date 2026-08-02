package com.jadhavr.erp.admission.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdmissionPrintAcademicSection(
        String academicYear,
        String departmentName,
        String departmentCode,
        String courseYearDisplayName,
        String previousSchoolName,
        String previousClassName,
        BigDecimal previousPercentage,
        List<AcademicRecordDto> academicRecords,
        List<EntranceExamDto> entranceExams,
        String qualifyingEntranceSeatNumber,
        BigDecimal qualifyingEntranceTotalScore,
        String lastGraduationCollegeName,
        String lastGraduationCollegeAddress
) {
}
