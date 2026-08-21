package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FeeReceiptResponse(
        Long paymentId,
        String receiptNumber,
        String receiptTitle,
        LocalDate issuedOn,
        Long collegeId,
        String collegeCode,
        String collegeName,
        String collegeLogoUrl,
        String collegeAddress,
        String collegeCity,
        String collegeState,
        String collegePincode,
        String collegeContactEmail,
        String collegeContactPhone,
        String studentName,
        String admissionNumber,
        String prn,
        String departmentName,
        String courseName,
        String academicYear,
        LocalDate paymentDate,
        BigDecimal amount,
        PaymentMode paymentMode,
        String transactionReference,
        LocalDateTime verifiedAt,
        String verifiedByName,
        String remark
) {
}
