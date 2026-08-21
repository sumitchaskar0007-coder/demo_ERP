package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;

public record PendingFeeRow(
        Long id,
        String studentName,
        String admissionNumber,
        String collegeName,
        String departmentName,
        String courseYear,
        String division,
        StudentCategory studentCategory,
        BigDecimal totalFee,
        BigDecimal paidAmount,
        BigDecimal remainingAmount
) {
}
