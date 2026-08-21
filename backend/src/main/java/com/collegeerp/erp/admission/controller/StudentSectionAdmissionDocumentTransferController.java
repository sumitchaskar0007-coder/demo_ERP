package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.AdmissionDocumentCompleteRequest;
import com.collegeerp.erp.admission.dto.AdmissionDocumentCompletionResponse;
import com.collegeerp.erp.admission.dto.AdmissionDocumentDownloadUrlResponse;
import com.collegeerp.erp.admission.dto.AdmissionDocumentPresignRequest;
import com.collegeerp.erp.admission.dto.AdmissionDocumentUploadResponse;
import com.collegeerp.erp.admission.service.AdmissionDocumentPresignedTransferService;
import com.collegeerp.erp.common.api.ApiResponse;
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
@RequestMapping("/api/student-section/admissions/{admissionId}/documents")
public class StudentSectionAdmissionDocumentTransferController {
    private final AdmissionDocumentPresignedTransferService transfers;

    public StudentSectionAdmissionDocumentTransferController(
            AdmissionDocumentPresignedTransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping("/{type}/presign")
    public ApiResponse<AdmissionDocumentUploadResponse> presignUpload(
            @PathVariable Long admissionId,
            @PathVariable String type,
            @Valid @RequestBody AdmissionDocumentPresignRequest request) {
        return ApiResponse.success(
                "Admission document upload URL created",
                transfers.initiate(admissionId, type, request));
    }

    @PostMapping("/{type}/complete")
    public ApiResponse<AdmissionDocumentCompletionResponse> completeUpload(
            @PathVariable Long admissionId,
            @PathVariable String type,
            @Valid @RequestBody AdmissionDocumentCompleteRequest request) {
        return ApiResponse.success(
                "Admission document upload verified",
                transfers.complete(admissionId, type, request.uploadId()));
    }

    @GetMapping("/{type}/download-url")
    public ApiResponse<AdmissionDocumentDownloadUrlResponse> downloadUrl(
            @PathVariable Long admissionId,
            @PathVariable String type) {
        return ApiResponse.success(
                "Admission document download URL created",
                transfers.download(admissionId, type));
    }
}
