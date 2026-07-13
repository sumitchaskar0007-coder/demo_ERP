package com.jadhavr.erp.fee.dto;

import java.math.BigDecimal;

public record PendingFeeSummary(BigDecimal totalPendingFee, Long pendingStudents) {
}
