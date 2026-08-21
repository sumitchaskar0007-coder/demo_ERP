package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.collegeerp.erp.admission.dto.StudentSectionAdmissionResponse;
import com.collegeerp.erp.admission.service.PrincipalAdmissionService;
import com.collegeerp.erp.admission.service.AdmissionPhotoService;
import com.collegeerp.erp.admission.service.AdmissionDocumentService;
import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.fee.dto.AdmissionFeeSummaryResponse;
import com.collegeerp.erp.fee.service.FeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
    private final AdmissionDocumentService documentService;
    private final FeeService feeService;

    public PrincipalAdmissionController(
            PrincipalAdmissionService principalAdmissionService,
            AdmissionPhotoService photoService,
            AdmissionDocumentService documentService,
            FeeService feeService) {
        this.principalAdmissionService = principalAdmissionService;
        this.photoService = photoService;
        this.documentService = documentService;
        this.feeService = feeService;
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

    @GetMapping("/{admissionId}/fees")
    public ApiResponse<AdmissionFeeSummaryResponse> getFees(@PathVariable Long admissionId) {
        principalAdmissionService.getAdmissionForPrincipal(admissionId);
        return ApiResponse.success("Course fee information retrieved",
                feeService.getAdmissionFeeSummary(admissionId));
    }
    @GetMapping("/{admissionId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long admissionId) {
        var photo = photoService.load(admissionId);
        return ResponseEntity.ok().contentType(photo.mediaType())
                .header("Content-Disposition", "inline; filename=\"student-photo\"")
                .body(photo.resource());
    }

    @GetMapping("/{admissionId}/documents/{type}")
    public ResponseEntity<Resource> getDocument(
            @PathVariable Long admissionId,
            @PathVariable String type) {
        var document = documentService.load(admissionId, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.filename(), java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(document.mediaType())
                .body(document.resource());
    }

}
