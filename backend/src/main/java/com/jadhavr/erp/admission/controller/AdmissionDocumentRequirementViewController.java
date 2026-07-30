package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionDocumentRequirementResponse;
import com.jadhavr.erp.admission.service.AdmissionDocumentRequirementService;
import com.jadhavr.erp.common.api.ApiResponse;
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
