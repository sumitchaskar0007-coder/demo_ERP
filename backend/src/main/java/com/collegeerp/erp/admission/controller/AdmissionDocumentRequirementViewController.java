package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementResponse;
import com.collegeerp.erp.admission.service.AdmissionDocumentRequirementService;
import com.collegeerp.erp.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admission-document-requirements")
public class AdmissionDocumentRequirementViewController {
    private final AdmissionDocumentRequirementService service;

    public AdmissionDocumentRequirementViewController(
            AdmissionDocumentRequirementService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ApiResponse<List<AdmissionDocumentRequirementResponse>> mine() {
        return ApiResponse.success("Admission document requirements fetched",
                service.activeForMine());
    }

    @GetMapping("/admission/{admissionId}")
    public ApiResponse<List<AdmissionDocumentRequirementResponse>> admission(
            @PathVariable Long admissionId) {
        return ApiResponse.success("Admission document requirements fetched",
                service.activeForAdmission(admissionId));
    }
}
