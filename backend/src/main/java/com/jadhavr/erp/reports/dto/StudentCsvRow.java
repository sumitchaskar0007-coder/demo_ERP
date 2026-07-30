package com.jadhavr.erp.reports.dto;

import com.jadhavr.erp.student.enums.StudentStatus;

public record StudentCsvRow(
        Long id,
        String studentName,
        String rollNumber,
        String email,
        String phone,
        String collegeName,
        String departmentName,
        StudentStatus status) {}
