package com.jadhavr.erp.fee.dto; import jakarta.validation.constraints.*; public record RejectPaymentRequest(@NotBlank @Size(min=5,max=500) String rejectionReason){}
