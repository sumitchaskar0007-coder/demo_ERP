package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionService;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.college.service.CollegeImageStorageService;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/admissions")
public class PublicAdmissionController {
    private final AdmissionService admissionService;
    private final CollegeRepository colleges;
    private final CollegeImageStorageService images;

    public PublicAdmissionController(AdmissionService admissionService, CollegeRepository colleges,
            CollegeImageStorageService images) {
        this.admissionService = admissionService;
        this.colleges = colleges;
        this.images = images;
    }

    @GetMapping("/college/{collegeCode}/logo")
    public ResponseEntity<org.springframework.core.io.Resource> collegeLogo(
            @PathVariable String collegeCode) {
        var college = colleges.findByCode(collegeCode.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
        if (college.getLogoUrl() == null) return ResponseEntity.notFound().build();
        var image = images.load(college.getLogoUrl());
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .header("Content-Disposition", "inline; filename=\"college-logo\"")
                .body(image.resource());
    }

    @GetMapping("/college/{collegeCode}/info")
    public ApiResponse<PublicAdmissionInfoResponse> getPublicAdmissionInfo(
            @PathVariable String collegeCode) {
        return ApiResponse.success(
                "Public admission info retrieved successfully",
                admissionService.getPublicAdmissionInfo(collegeCode)
        );
    }

    @PostMapping("/college/{collegeCode}/submit")
    public ResponseEntity<ApiResponse<SubmitAdmissionResponse>> submitAdmission(
            @PathVariable String collegeCode,
            @Valid @RequestBody SubmitAdmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Admission submitted successfully",
                        admissionService.submitAdmission(collegeCode, request)
                ));
    }
}
