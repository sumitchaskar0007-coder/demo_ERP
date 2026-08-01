package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionPrintResponse;
import com.jadhavr.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.jadhavr.erp.admission.dto.AdmissionStatusHistoryResponse;
import com.jadhavr.erp.admission.dto.MarkAdmissionPrintedRequest;
import com.jadhavr.erp.admission.dto.RejectAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.dto.VerifyAdmissionRequest;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.admission.service.AdmissionDocumentService;
import com.jadhavr.erp.admission.service.StudentSectionAdmissionService;
import com.jadhavr.erp.admission.service.AdmissionDocumentCustodyService;
import com.jadhavr.erp.admission.dto.AdmissionDocumentCustodyResponse;
import com.jadhavr.erp.admission.dto.ReturnAdmissionDocumentRequest;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.fee.dto.AdmissionFeeSummaryResponse;
import com.jadhavr.erp.fee.service.FeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/student-section/admissions")
public class StudentSectionAdmissionController {
    private final StudentSectionAdmissionService admissionService;
    private final AdmissionPhotoService photoService;
    private final AdmissionDocumentService documentService;
    private final FeeService feeService;
    private final AdmissionDocumentCustodyService custodyService;

    public StudentSectionAdmissionController(
            StudentSectionAdmissionService admissionService,
            AdmissionPhotoService photoService,
            AdmissionDocumentService documentService,
            FeeService feeService, AdmissionDocumentCustodyService custodyService) {
        this.admissionService = admissionService;
        this.photoService = photoService;
        this.documentService = documentService;
        this.feeService = feeService;
        this.custodyService = custodyService;
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

    @GetMapping("/{admissionId}/fees")
    public ApiResponse<AdmissionFeeSummaryResponse> getFees(@PathVariable Long admissionId) {
        admissionService.getAdmissionForStudentSection(admissionId);
        return ApiResponse.success("Student fee information retrieved",
                feeService.getAdmissionFeeSummary(admissionId));
    }

    @GetMapping("/{admissionId}/course-years")
    public ApiResponse<List<AdmissionCourseYearOptionResponse>> getCourseYears(@PathVariable Long admissionId) {
        return ApiResponse.success("Available course years retrieved", admissionService.getCourseYearOptions(admissionId));
    }

    @PatchMapping("/{admissionId}/start-review")
    public ApiResponse<StudentSectionAdmissionResponse> startReview(@PathVariable Long admissionId) {
        return ApiResponse.success(
                "Admission review started successfully",
                admissionService.startReview(admissionId)
        );
    }
    @PutMapping("/{admissionId}/details")
    public ApiResponse<StudentSectionAdmissionResponse> updateDetails(
            @PathVariable Long admissionId,
            @Valid @RequestBody DetailedAdmissionRequest request) {
        return ApiResponse.success(
                "Detailed admission form saved successfully",
                admissionService.updateDetails(admissionId, request)
        );
    }

    @PatchMapping("/{admissionId}/approve")
    public ApiResponse<StudentSectionAdmissionResponse> approveAdmission(
            @PathVariable Long admissionId,
            @Valid @RequestBody VerifyAdmissionRequest request) {
        var response = admissionService.approveAdmission(admissionId, request);
        custodyService.record(admissionId, request.documentCustody());
        return ApiResponse.success(
                "Admission approved by Student Section successfully",
                response
        );
    }

    @GetMapping("/{admissionId}/document-custody")
    public ApiResponse<List<AdmissionDocumentCustodyResponse>> custody(@PathVariable Long admissionId) {
        return ApiResponse.success("Document custody retrieved", custodyService.list(admissionId));
    }

    @PatchMapping("/{admissionId}/document-custody/{documentType}/returned")
    public ApiResponse<AdmissionDocumentCustodyResponse> returned(@PathVariable Long admissionId,
            @PathVariable String documentType, @Valid @RequestBody ReturnAdmissionDocumentRequest request) {
        return ApiResponse.success("Document return updated",
                custodyService.returned(admissionId, documentType, request));
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
    @PostMapping(path = "/{admissionId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadPhoto(
            @PathVariable Long admissionId, @RequestParam("file") MultipartFile file) {
        photoService.save(admissionId, file);
        return ApiResponse.success(
                "Student photo uploaded successfully",
                admissionService.getAdmissionForStudentSection(admissionId));
    }

    @GetMapping("/{admissionId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable Long admissionId) {
        var photo = photoService.load(admissionId);
        return ResponseEntity.ok().contentType(photo.mediaType())
                .header("Content-Disposition", "inline; filename=\"student-photo\"")
                .body(photo.resource());
    }

    @DeleteMapping("/{admissionId}/photo")
    public ApiResponse<Void> deletePhoto(@PathVariable Long admissionId) {
        photoService.delete(admissionId);
        return ApiResponse.success("Student photo removed successfully", null);
    }

    @PostMapping(path = "/{admissionId}/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadDocument(
            @PathVariable Long admissionId, @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        documentService.save(admissionId, type, file);
        return ApiResponse.success("Admission document uploaded successfully",
                admissionService.getAdmissionForStudentSection(admissionId));
    }

    @GetMapping("/{admissionId}/documents/{type}")
    public ResponseEntity<Resource> getDocument(
            @PathVariable Long admissionId, @PathVariable String type) {
        var document = documentService.load(admissionId, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.filename(), java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(document.mediaType())
                .body(document.resource());
    }

}
