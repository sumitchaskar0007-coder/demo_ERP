package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.FeeAccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScholarshipResponse(
        Long feeAccountId,
        Long studentId,
        String studentName,
        String admissionNumber,
        BigDecimal totalFee,
        BigDecimal paidAmount,
        BigDecimal scholarshipAmount,
        BigDecimal remainingAmount,
        FeeAccountStatus status,
        String approvedBy,
        LocalDateTime approvedAt) {
}
