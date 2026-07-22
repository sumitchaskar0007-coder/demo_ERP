package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMode paymentMode,
        @NotBlank @Size(min = 3, max = 100) String transactionReference,
        @NotNull LocalDate paymentDate,
        @Size(max = 500) String remarks
) {}
