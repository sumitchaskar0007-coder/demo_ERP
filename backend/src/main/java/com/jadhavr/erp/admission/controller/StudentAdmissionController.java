package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionService;
import com.jadhavr.erp.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/admissions")
public class StudentAdmissionController {
    private final AdmissionService admissionService;

    public StudentAdmissionController(AdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @GetMapping("/me")
    public ApiResponse<AdmissionResponse> getMyLatestAdmission() {
        return ApiResponse.success(
                "Admission retrieved successfully",
                admissionService.getMyLatestAdmission()
        );
    }
}
