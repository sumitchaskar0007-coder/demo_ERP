package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.FeeAccountStatus;

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
        BigDecimal creditAmount,
        boolean scholarshipRemoved,
        LocalDateTime scholarshipRemovedAt,
        String scholarshipRemovalReason,
        FeeAccountStatus status,
        String approvedBy,
        LocalDateTime approvedAt) {
}
