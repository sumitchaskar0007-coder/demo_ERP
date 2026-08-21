package com.collegeerp.erp.reports.dto;

import com.collegeerp.erp.fee.enums.FeeAccountStatus;

import java.math.BigDecimal;

public record FeeCsvRow(
        Long id,
        String studentName,
        String admissionNumber,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        FeeAccountStatus status) {}
