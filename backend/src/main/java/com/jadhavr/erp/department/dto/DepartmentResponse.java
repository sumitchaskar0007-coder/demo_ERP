package com.jadhavr.erp.department.dto;

import com.jadhavr.erp.department.entity.DepartmentStatus;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        Long collegeId,
        String collegeName,
        String collegeCode,
        String name,
        String code,
        String description,
        DepartmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
