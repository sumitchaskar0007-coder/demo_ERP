package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.fee.dto.ChangeStudentCategoryRequest;
import com.collegeerp.erp.fee.dto.FeeCategoryAssessmentOptionResponse;
import com.collegeerp.erp.fee.dto.RemoveScholarshipRequest;
import com.collegeerp.erp.fee.dto.ScholarshipResponse;
import com.collegeerp.erp.fee.dto.StudentFeeAccountResponse;
import com.collegeerp.erp.fee.service.PrincipalFeeAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/principal/students/{studentId}/fee-adjustments")
public class PrincipalFeeAdjustmentController {
    private final PrincipalFeeAdjustmentService adjustments;

    public PrincipalFeeAdjustmentController(PrincipalFeeAdjustmentService adjustments) {
        this.adjustments = adjustments;
    }

    @GetMapping("/category-options")
    public ApiResponse<List<FeeCategoryAssessmentOptionResponse>> categoryOptions(
            @PathVariable Long studentId) {
        return ApiResponse.success("Applicable fee categories retrieved",
                adjustments.categoryOptions(studentId));
    }

    @PostMapping("/scholarship/remove")
    public ApiResponse<ScholarshipResponse> removeScholarship(
            @PathVariable Long studentId,
            @Valid @RequestBody RemoveScholarshipRequest request) {
        return ApiResponse.success("Student scholarship removed",
                adjustments.removeScholarship(studentId, request));
    }

    @PatchMapping("/category")
    public ApiResponse<StudentFeeAccountResponse> changeCategory(
            @PathVariable Long studentId,
            @Valid @RequestBody ChangeStudentCategoryRequest request) {
        return ApiResponse.success("Student category and fee assessment updated",
                adjustments.changeCategory(studentId, request));
    }
}
