package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionDocumentRequirementRequest;
import com.jadhavr.erp.admission.dto.AdmissionDocumentRequirementResponse;
import com.jadhavr.erp.admission.service.AdmissionDocumentRequirementService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/college-settings/admission-documents")
public class AdmissionDocumentRequirementController {
    private final AdmissionDocumentRequirementService service;

    public AdmissionDocumentRequirementController(AdmissionDocumentRequirementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AdmissionDocumentRequirementResponse>> list(
            @RequestParam(required = false) Long collegeId) {
        return ApiResponse.success("Admission document settings fetched",
                service.settings(collegeId));
    }

    @PostMapping
    public ApiResponse<AdmissionDocumentRequirementResponse> create(
            @RequestParam(required = false) Long collegeId,
            @Valid @RequestBody AdmissionDocumentRequirementRequest request) {
        return ApiResponse.success("Admission document added", service.create(collegeId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdmissionDocumentRequirementResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) Long collegeId,
            @Valid @RequestBody AdmissionDocumentRequirementRequest request) {
        return ApiResponse.success("Admission document updated",
                service.update(id, collegeId, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdmissionDocumentRequirementResponse> setActive(
            @PathVariable Long id,
            @RequestParam(required = false) Long collegeId,
            @RequestParam boolean active) {
        return ApiResponse.success("Admission document status updated",
                service.setActive(id, collegeId, active));
    }
}
