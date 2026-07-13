package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeCollectionRow(
        Long id,
        String studentName,
        String collegeName,
        String departmentName,
        StudentCategory studentCategory,
        BigDecimal amount,
        LocalDate paymentDate,
        String transactionReference
) {
}
