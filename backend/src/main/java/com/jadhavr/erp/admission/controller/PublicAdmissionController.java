package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionService;
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

    public PublicAdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
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
