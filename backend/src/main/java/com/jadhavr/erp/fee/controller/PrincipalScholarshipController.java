package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.fee.dto.ScholarshipResponse;
import com.jadhavr.erp.fee.service.ScholarshipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.jadhavr.erp.fee.dto.ApproveScholarshipRequest;

@RestController
@RequestMapping("/api/principal/students/{studentId}/scholarship")
public class PrincipalScholarshipController {
    private final ScholarshipService scholarships;

    public PrincipalScholarshipController(ScholarshipService scholarships) {
        this.scholarships = scholarships;
    }

    @GetMapping
    public ApiResponse<ScholarshipResponse> details(@PathVariable Long studentId) {
        return ApiResponse.success("Student scholarship details retrieved",
                scholarships.details(studentId));
    }

    @PostMapping("/approve")
    public ApiResponse<ScholarshipResponse> approve(
            @PathVariable Long studentId,
            @Valid @RequestBody ApproveScholarshipRequest request) {
        return ApiResponse.success("Student scholarship approved",
                scholarships.approve(studentId, request));
    }

}
