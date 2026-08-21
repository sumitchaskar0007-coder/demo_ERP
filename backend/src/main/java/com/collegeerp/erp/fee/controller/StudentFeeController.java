package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.fee.dto.FeeTransactionResponse;
import com.collegeerp.erp.fee.dto.FeeReceiptResponse;
import com.collegeerp.erp.fee.dto.PaymentResponse;
import com.collegeerp.erp.fee.dto.StudentFeeAccountResponse;
import com.collegeerp.erp.fee.dto.SubmitPaymentRequest;
import com.collegeerp.erp.fee.service.FeeService;
import com.collegeerp.erp.fee.service.FeeReceiptService;
import com.collegeerp.erp.fee.service.PaymentProofStorageService;
import com.collegeerp.erp.college.service.CollegeImageStorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/student/fees")
public class StudentFeeController {
    private final FeeService service;
    private final PaymentProofStorageService proofStorage;
    private final CollegeImageStorageService collegeImages;
    private final FeeReceiptService receipts;

    public StudentFeeController(FeeService service, PaymentProofStorageService proofStorage,
            CollegeImageStorageService collegeImages, FeeReceiptService receipts) {
        this.service = service;
        this.proofStorage = proofStorage;
        this.collegeImages = collegeImages;
        this.receipts = receipts;
    }

    @GetMapping("/me")
    public ApiResponse<StudentFeeAccountResponse> me() {
        return ApiResponse.success("Fee account retrieved", service.myAccount());
    }

    @GetMapping("/payment-qr")
    public ResponseEntity<Resource> paymentQr() {
        var image = collegeImages.load(service.myCollegeQrStorageName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=payment-qr")
                .contentType(image.mediaType())
                .body(image.resource());
    }

    @PostMapping(path = "/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PaymentResponse>> submit(
            @Valid @RequestPart("request") SubmitPaymentRequest request,
            @RequestPart("proof") MultipartFile proof) {
        String storageName = proofStorage.save(proof);
        try {
            PaymentResponse payment = service.submitPayment(request, storageName);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Payment proof submitted", payment));
        } catch (RuntimeException exception) {
            proofStorage.delete(storageName);
            throw exception;
        }
    }

    @GetMapping("/payments/{paymentId}/proof")
    public ResponseEntity<Resource> proof(@PathVariable Long paymentId) {
        var proof = proofStorage.load(service.myPaymentProofStorageName(paymentId));
        return proofResponse(proof, paymentId);
    }

    @GetMapping("/payments")
    public ApiResponse<List<PaymentResponse>> payments() {
        return ApiResponse.success("Payments retrieved", service.myPayments());
    }

    @GetMapping("/payments/{paymentId}/receipt")
    public ApiResponse<FeeReceiptResponse> receipt(@PathVariable Long paymentId) {
        return ApiResponse.success("Payment receipt retrieved",
                receipts.currentStudentReceipt(paymentId));
    }

    @GetMapping("/transactions")
    public ApiResponse<List<FeeTransactionResponse>> transactions() {
        return ApiResponse.success("Transactions retrieved", service.myTransactions());
    }

    private ResponseEntity<Resource> proofResponse(
            PaymentProofStorageService.PaymentProofResource proof, Long paymentId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("payment-proof-" + paymentId, StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(proof.mediaType())
                .body(proof.resource());
    }
}
