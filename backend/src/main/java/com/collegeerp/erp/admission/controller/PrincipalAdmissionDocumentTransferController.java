package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.AdmissionDocumentDownloadUrlResponse;
import com.collegeerp.erp.admission.service.AdmissionDocumentPresignedTransferService;
import com.collegeerp.erp.common.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("preprod | production")
@RequestMapping("/api/principal/admissions/{admissionId}/documents")
public class PrincipalAdmissionDocumentTransferController {
    private final AdmissionDocumentPresignedTransferService transfers;

    public PrincipalAdmissionDocumentTransferController(
            AdmissionDocumentPresignedTransferService transfers) {
        this.transfers = transfers;
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
