package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementRequest;
import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementResponse;
import com.collegeerp.erp.admission.service.AdmissionDocumentRequirementService;
import com.collegeerp.erp.common.api.ApiResponse;
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
            @RequestParam(required = false) Long collegeId,
            @RequestParam Long departmentId) {
        return ApiResponse.success("Admission document settings fetched",
                service.settings(collegeId, departmentId));
    }

    @PostMapping
    public ApiResponse<AdmissionDocumentRequirementResponse> create(
            @RequestParam(required = false) Long collegeId,
            @RequestParam Long departmentId,
            @Valid @RequestBody AdmissionDocumentRequirementRequest request) {
        return ApiResponse.success("Admission document added",
                service.create(collegeId, departmentId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdmissionDocumentRequirementResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) Long collegeId,
            @RequestParam Long departmentId,
            @Valid @RequestBody AdmissionDocumentRequirementRequest request) {
        return ApiResponse.success("Admission document updated",
                service.update(id, collegeId, departmentId, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<AdmissionDocumentRequirementResponse> setActive(
            @PathVariable Long id,
            @RequestParam(required = false) Long collegeId,
            @RequestParam Long departmentId,
            @RequestParam boolean active) {
        return ApiResponse.success("Admission document status updated",
                service.setActive(id, collegeId, departmentId, active));
    }
}
