package com.collegeerp.erp.fee.dto;

import java.math.BigDecimal;

public record FeeBalanceTotals(
        BigDecimal totalPaid,
        BigDecimal totalRemaining
) {
}
