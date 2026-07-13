package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;

public record PendingFeeRow(
        Long id,
        String studentName,
        String admissionNumber,
        String collegeName,
        String departmentName,
        StudentCategory studentCategory,
        BigDecimal totalFee,
        BigDecimal paidAmount,
        BigDecimal remainingAmount
) {
}
