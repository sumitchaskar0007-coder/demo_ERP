package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.admission.service.AdmissionDocumentService;
import com.jadhavr.erp.admission.service.AdmissionDocumentService.DocumentType;
import com.jadhavr.erp.admission.service.StudentSectionAdmissionService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student-section/admissions")
public class StudentSectionAdmissionController {
    private final StudentSectionAdmissionService admissionService;
    private final AdmissionPhotoService photoService;
    private final AdmissionDocumentService documentService;

    public StudentSectionAdmissionController(
            StudentSectionAdmissionService admissionService,
            AdmissionPhotoService photoService,
            AdmissionDocumentService documentService) {
        this.admissionService = admissionService;
        this.photoService = photoService;
        this.documentService = documentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentSectionAdmissionResponse>> searchAdmissions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) AdmissionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Admissions searched successfully",
                admissionService.searchAdmissionsForStudentSection(
                        keyword, departmentId, status, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{admissionId}")
    public ApiResponse<StudentSectionAdmissionResponse> getAdmission(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission retrieved successfully",
                admissionService.getAdmissionForStudentSection(admissionId)
        );
    }

    @PatchMapping("/{admissionId}/start-review")
    public ApiResponse<StudentSectionAdmissionResponse> startReview(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission review started successfully",
                admissionService.startReview(admissionId)
        );
    }
    @PatchMapping("/{admissionId}/approve")
    public ApiResponse<StudentSectionAdmissionResponse> approveAdmission(
            @PathVariable Long admissionId,
            @Valid @RequestBody VerifyAdmissionRequest request) {
        return ApiResponse.success(
                "Admission approved by Student Section successfully",
                admissionService.approveAdmission(admissionId, request)
        );
    }

    @PatchMapping("/{admissionId}/reject")
    public ApiResponse<StudentSectionAdmissionResponse> rejectAdmission(
            @PathVariable Long admissionId,
            @Valid @RequestBody RejectAdmissionRequest request) {
        return ApiResponse.success(
                "Admission rejected by Student Section successfully",
                admissionService.rejectAdmission(admissionId, request)
        );
    }

    @GetMapping("/{admissionId}/history")
    public ApiResponse<List<AdmissionStatusHistoryResponse>> getHistory(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission history retrieved successfully",
                admissionService.getAdmissionHistory(admissionId)
        );
    }

    @GetMapping("/{admissionId}/print-data")
    @PreAuthorize("hasRole('STUDENT_SECTION')")
    public ApiResponse<AdmissionPrintResponse> getPrintData(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission print data retrieved successfully",
                admissionService.getPrintData(admissionId)
        );
    }

    @PatchMapping("/{admissionId}/mark-printed")
    @PreAuthorize("hasRole('STUDENT_SECTION')")
    public ApiResponse<StudentSectionAdmissionResponse> markPrinted(
            @PathVariable Long admissionId,
            @Valid @RequestBody MarkAdmissionPrintedRequest request) {
        return ApiResponse.success(
                "Admission marked as printed successfully",
                admissionService.markAdmissionPrinted(admissionId, request)
        );
    }
    @GetMapping("/{admissionId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long admissionId) {
        var photo = photoService.load(admissionId);
        return ResponseEntity.ok().contentType(photo.mediaType()).body(photo.resource());
    }

    @GetMapping("/{admissionId}/documents/{type}")
    public ResponseEntity<Resource> getDocument(
            @PathVariable Long admissionId, @PathVariable DocumentType type) {
        var document = documentService.load(admissionId, type);
        return ResponseEntity.ok().contentType(document.mediaType()).body(document.resource());
    }

}
