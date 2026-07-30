package com.jadhavr.erp.fee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ApproveScholarshipRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Scholarship amount must be greater than zero")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
        @Size(max = 500)
        String remarks) {
}
