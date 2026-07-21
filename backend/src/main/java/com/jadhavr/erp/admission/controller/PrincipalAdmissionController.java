package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.service.PrincipalAdmissionService;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/principal/admissions")
public class PrincipalAdmissionController {
    private final PrincipalAdmissionService principalAdmissionService;
    private final AdmissionPhotoService photoService;

    public PrincipalAdmissionController(
            PrincipalAdmissionService principalAdmissionService,
            AdmissionPhotoService photoService) {
        this.principalAdmissionService = principalAdmissionService;
        this.photoService = photoService;
    }

    @GetMapping("/review-ready")
    public ApiResponse<PageResponse<StudentSectionAdmissionResponse>> getReviewReadyAdmissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Review-ready admissions retrieved successfully",
                principalAdmissionService.getReviewReadyAdmissions(
                        keyword, departmentId, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{admissionId}")
    public ApiResponse<StudentSectionAdmissionResponse> getAdmission(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission retrieved successfully",
                principalAdmissionService.getAdmissionForPrincipal(admissionId)
        );
    }

    @GetMapping("/{admissionId}/history")
    public ApiResponse<List<AdmissionStatusHistoryResponse>> getHistory(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission history retrieved successfully",
                principalAdmissionService.getAdmissionHistoryForPrincipal(admissionId)
        );
    }
    @GetMapping("/{admissionId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long admissionId) {
        var photo = photoService.load(admissionId);
        return ResponseEntity.ok().contentType(photo.mediaType())
                .header("Content-Disposition", "inline; filename=\"student-photo\"")
                .body(photo.resource());
    }

}
