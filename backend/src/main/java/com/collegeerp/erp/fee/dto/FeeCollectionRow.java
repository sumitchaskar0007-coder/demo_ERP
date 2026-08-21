package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeCollectionRow(
        Long id,
        String studentName,
        String collegeName,
        String departmentName,
        String courseYear,
        String division,
        StudentCategory studentCategory,
        BigDecimal amount,
        LocalDate paymentDate,
        String transactionReference
) {
}
