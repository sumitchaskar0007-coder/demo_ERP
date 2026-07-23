package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.fee.dto.FeeTransactionResponse;
import com.jadhavr.erp.fee.dto.PaymentResponse;
import com.jadhavr.erp.fee.dto.StudentFeeAccountResponse;
import com.jadhavr.erp.fee.dto.SubmitPaymentRequest;
import com.jadhavr.erp.fee.service.FeeService;
import com.jadhavr.erp.fee.service.PaymentProofStorageService;
import com.jadhavr.erp.college.service.CollegeImageStorageService;
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

    public StudentFeeController(FeeService service, PaymentProofStorageService proofStorage,
            CollegeImageStorageService collegeImages) {
        this.service = service;
        this.proofStorage = proofStorage;
        this.collegeImages = collegeImages;
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

    @GetMapping("/transactions")
    public ApiResponse<List<FeeTransactionResponse>> transactions() {
        return ApiResponse.success("Transactions retrieved", service.myTransactions());
    }

    private ResponseEntity<Resource> proofResponse(
            PaymentProofStorageService.PaymentProofResource proof, Long paymentId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("payment-proof-" + paymentId, StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(proof.mediaType())
                .body(proof.resource());
    }
}
