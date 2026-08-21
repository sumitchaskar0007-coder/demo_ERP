package com.collegeerp.erp.academic.dto;

public record StudentAcademicAccessResponse(
        boolean divisionAllocated,
        Long courseYearId,
        String courseYear,
        Long divisionId,
        String division,
        String academicYear,
        String rollNumber
) {
    public static StudentAcademicAccessResponse notAllocated() {
        return new StudentAcademicAccessResponse(false, null, null, null, null, null, null);
    }
}
