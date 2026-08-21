package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.dto.DetailedAdmissionRequest;
import com.collegeerp.erp.admission.dto.AdmissionPrintResponse;
import com.collegeerp.erp.admission.dto.AdmissionCourseYearOptionResponse;
import com.collegeerp.erp.admission.enums.AdmissionDocumentType;
import com.collegeerp.erp.admission.dto.StudentAdmissionAccessResponse;
import com.collegeerp.erp.admission.dto.StudentSectionAdmissionResponse;
import com.collegeerp.erp.admission.service.AdmissionPhotoService;
import com.collegeerp.erp.admission.service.AdmissionService;
import com.collegeerp.erp.admission.service.AdmissionDocumentService;
import com.collegeerp.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student/admissions")
public class StudentAdmissionController {
    private final AdmissionService admissionService;
    private final AdmissionPhotoService photoService;
    private final AdmissionDocumentService documentService;

    public StudentAdmissionController(AdmissionService admissionService, AdmissionPhotoService photoService,
            AdmissionDocumentService documentService) {
        this.admissionService = admissionService;
        this.photoService = photoService;
        this.documentService = documentService;
    }

    @GetMapping("/me")
    public ApiResponse<StudentSectionAdmissionResponse> getMyLatestAdmission() {
        return ApiResponse.success(
                "Admission retrieved successfully",
                admissionService.getMyDetailedAdmission()
        );
    }

    @GetMapping("/me/print-data")
    public ApiResponse<AdmissionPrintResponse> getMyPrintData() {
        return ApiResponse.success(
                "Admission print data retrieved successfully",
                admissionService.getMyAdmissionPrintData());
    }

    @GetMapping("/access-state")
    public ApiResponse<StudentAdmissionAccessResponse> accessState() {
        return ApiResponse.success("Student admission access retrieved", admissionService.getMyAdmissionAccess());
    }

    @GetMapping("/me/course-years")
    public ApiResponse<java.util.List<AdmissionCourseYearOptionResponse>> courseYears() {
        return ApiResponse.success("Available course years retrieved", admissionService.getMyCourseYearOptions());
    }

    @PutMapping("/me/details")
    public ApiResponse<StudentSectionAdmissionResponse> submitDetails(
            @Valid @RequestBody DetailedAdmissionRequest request) {
        return ApiResponse.success(
                "Admission form submitted successfully and is pending review",
                admissionService.submitMyAdmissionDetails(request));
    }

    @PostMapping("/me/details/validate")
    public ApiResponse<com.collegeerp.erp.admission.dto.AdmissionInformationValidationResponse>
            validateDetails(@Valid @RequestBody DetailedAdmissionRequest request) {
        return ApiResponse.success(
                "Admission information is valid",
                admissionService.validateMyAdmissionDetails(request));
    }

    @GetMapping("/me/details/draft")
    public ApiResponse<com.collegeerp.erp.admission.dto.AdmissionDetailDraftResponse> getDraft() {
        return ApiResponse.success("Admission draft retrieved", admissionService.getMyAdmissionDetailDraft());
    }

    @PutMapping("/me/details/draft")
    public ApiResponse<com.collegeerp.erp.admission.dto.AdmissionDetailDraftResponse> saveDraft(
            @Valid @RequestBody com.collegeerp.erp.admission.dto.AdmissionDetailDraftRequest request) {
        return ApiResponse.success("Admission draft saved", admissionService.saveMyAdmissionDetailDraft(request));
    }

    @PostMapping(path = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadPhoto(@RequestParam("file") MultipartFile file) {
        photoService.saveMine(file);
        return ApiResponse.success("Student photo uploaded successfully", admissionService.getMyDetailedAdmission());
    }

    @GetMapping("/me/photo")
    public ResponseEntity<Resource> getPhoto() {
        var photo = photoService.loadMine();
        return ResponseEntity.ok().contentType(photo.mediaType())
                .header("Content-Disposition", "inline; filename=\"student-photo\"")
                .body(photo.resource());
    }

    @DeleteMapping("/me/photo")
    public ApiResponse<Void> deletePhoto() {
        photoService.deleteMine();
        return ApiResponse.success("Student photo removed successfully", null);
    }

    @PostMapping(path = "/me/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadDocument(
            @PathVariable String type, @RequestParam("file") MultipartFile file) {
        documentService.saveMine(type, file);
        return ApiResponse.success("Admission document uploaded successfully", admissionService.getMyDetailedAdmission());
    }

    @GetMapping("/me/documents/{type}")
    public ResponseEntity<Resource> getDocument(@PathVariable String type) {
        var document = documentService.loadMine(type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.filename(), java.nio.charset.StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(document.mediaType())
                .body(document.resource());
    }
}
