package com.collegeerp.erp.fee.dto; import jakarta.validation.constraints.Size; public record VerifyPaymentRequest(@Size(max=500) String remarks){}
