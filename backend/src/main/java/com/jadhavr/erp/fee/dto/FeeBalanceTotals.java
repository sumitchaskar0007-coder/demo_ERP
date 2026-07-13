package com.jadhavr.erp.fee.dto;

import java.math.BigDecimal;

public record FeeBalanceTotals(
        BigDecimal totalPaid,
        BigDecimal totalRemaining
) {
}
