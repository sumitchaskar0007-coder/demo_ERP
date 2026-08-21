package com.collegeerp.erp.fee.dto;

import java.math.BigDecimal;

public record PendingFeeSummary(BigDecimal totalPendingFee, Long pendingStudents) {
}
