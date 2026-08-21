package com.collegeerp.erp.fee.dto;

import java.util.List;

public record AdmissionFeeSummaryResponse(
        StudentFeeAccountResponse account,
        List<PaymentResponse> payments) {
}
