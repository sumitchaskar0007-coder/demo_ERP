package com.collegeerp.erp.reports.dto;

import com.collegeerp.erp.student.enums.StudentStatus;

public record StudentCsvRow(
        Long id,
        String studentName,
        String rollNumber,
        String email,
        String phone,
        String collegeName,
        String departmentName,
        StudentStatus status) {}
