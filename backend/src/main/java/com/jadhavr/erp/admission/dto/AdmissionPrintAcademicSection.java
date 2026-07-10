package com.jadhavr.erp.admission.dto;

import java.math.BigDecimal;

public record AdmissionPrintAcademicSection(
        String academicYear,
        String departmentName,
        String departmentCode,
        String previousSchoolName,
        String previousClassName,
        BigDecimal previousPercentage
) {
}
