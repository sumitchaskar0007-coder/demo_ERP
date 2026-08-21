package com.collegeerp.erp.department.dto;

import com.collegeerp.erp.department.entity.DepartmentStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record DepartmentResponse(
        Long id,
        Long collegeId,
        String collegeName,
        String collegeCode,
        String name,
        String code,
        String description,
        BigDecimal admissionFormFee,
        DepartmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
