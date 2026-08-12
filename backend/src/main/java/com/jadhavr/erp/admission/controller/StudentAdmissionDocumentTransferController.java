package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionDocumentCompleteRequest;
import com.jadhavr.erp.admission.dto.AdmissionDocumentCompletionResponse;
import com.jadhavr.erp.admission.dto.AdmissionDocumentDownloadUrlResponse;
import com.jadhavr.erp.admission.dto.AdmissionDocumentPresignRequest;
import com.jadhavr.erp.admission.dto.AdmissionDocumentUploadResponse;
import com.jadhavr.erp.admission.service.AdmissionDocumentPresignedTransferService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("preprod | production")
@RequestMapping("/api/student/admissions/me/documents")
public class StudentAdmissionDocumentTransferController {
    private final AdmissionDocumentPresignedTransferService transfers;

    public StudentAdmissionDocumentTransferController(
            AdmissionDocumentPresignedTransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping("/{type}/presign")
    public ApiResponse<AdmissionDocumentUploadResponse> presignUpload(
            @PathVariable String type,
            @Valid @RequestBody AdmissionDocumentPresignRequest request) {
        return ApiResponse.success(
                "Admission document upload URL created",
                transfers.initiateMine(type, request));
    }

    @PostMapping("/{type}/complete")
    public ApiResponse<AdmissionDocumentCompletionResponse> completeUpload(
            @PathVariable String type,
            @Valid @RequestBody AdmissionDocumentCompleteRequest request) {
        return ApiResponse.success(
                "Admission document upload verified",
                transfers.completeMine(type, request.uploadId()));
    }

    @GetMapping("/{type}/download-url")
    public ApiResponse<AdmissionDocumentDownloadUrlResponse> downloadUrl(
            @PathVariable String type) {
        return ApiResponse.success(
                "Admission document download URL created",
                transfers.downloadMine(type));
    }
}
