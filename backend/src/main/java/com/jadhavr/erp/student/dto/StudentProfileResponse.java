package com.jadhavr.erp.student.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.student.enums.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentProfileResponse(
        Long id,
        Long userId,
        String admissionNumber,
        StudentCategory studentCategory,
        String customCategoryName,
        Long collegeId,
        String collegeName,
        String collegeCode,
        Long departmentId,
        String departmentName,
        String departmentCode,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String parentName,
        String parentPhone,
        StudentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
