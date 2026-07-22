package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.fee.dto.*;
import com.jadhavr.erp.fee.enums.*;
import com.jadhavr.erp.fee.service.FeeService;
import com.jadhavr.erp.fee.service.PaymentProofStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-section")
public class FeeSectionController {
    private final FeeService service;
    private final PaymentProofStorageService proofStorage;

    public FeeSectionController(FeeService service, PaymentProofStorageService proofStorage) {
        this.service = service;
        this.proofStorage = proofStorage;
    }

    @GetMapping("/dashboard")
    public ApiResponse<FeeDashboardResponse> dashboard() {
        return ApiResponse.success("Fee dashboard retrieved", service.dashboard());
    }

    @GetMapping("/fee-accounts")
    public ApiResponse<PageResponse<StudentFeeAccountResponse>> accounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) FeeAccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Fee accounts retrieved",
                service.searchAccounts(keyword, collegeId, departmentId, status, page, size));
    }

    @GetMapping("/fee-accounts/{id}")
    public ApiResponse<StudentFeeAccountResponse> account(@PathVariable Long id) {
        return ApiResponse.success("Fee account retrieved", service.getAccount(id));
    }

    @PostMapping("/fee-accounts/{id}/send-reminder")
    @PreAuthorize("hasRole('FEE_SECTION')")
    public ApiResponse<Void> reminder(@PathVariable Long id) {
        service.sendPendingFeeReminder(id);
        return ApiResponse.success("Pending fee reminder queued to the student email", null);
    }

    @GetMapping("/fee-accounts/{id}/transactions")
    public ApiResponse<List<FeeTransactionResponse>> transactions(@PathVariable Long id) {
        return ApiResponse.success("Transactions retrieved", service.accountTransactions(id));
    }

    @GetMapping("/payments")
    public ApiResponse<PageResponse<PaymentResponse>> payments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Payments retrieved",
                service.searchPayments(keyword, collegeId, departmentId, status, page, size));
    }

    @GetMapping("/payments/{id}")
    public ApiResponse<PaymentResponse> payment(@PathVariable Long id) {
        return ApiResponse.success("Payment retrieved", service.getPayment(id));
    }

    @GetMapping("/payments/{id}/proof")
    public ResponseEntity<Resource> paymentProof(@PathVariable Long id) {
        var proof = proofStorage.load(service.paymentProofStorageName(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("payment-proof-" + id, java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(proof.mediaType())
                .body(proof.resource());
    }

    @PatchMapping("/payments/{id}/verify")
    public ApiResponse<PaymentResponse> verify(@PathVariable Long id,
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.success("Payment verified", service.verify(id, request));
    }

    @PatchMapping("/payments/{id}/reject")
    public ApiResponse<PaymentResponse> reject(@PathVariable Long id,
            @Valid @RequestBody RejectPaymentRequest request) {
        return ApiResponse.success("Payment rejected", service.reject(id, request));
    }
}
