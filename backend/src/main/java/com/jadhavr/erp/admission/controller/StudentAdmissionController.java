package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.AdmissionResponse;
import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionService;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.admission.service.AdmissionDocumentService;
import com.jadhavr.erp.admission.service.AdmissionDocumentService.DocumentType;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ApiResponse<AdmissionResponse> getMyLatestAdmission() {
        return ApiResponse.success(
                "Admission retrieved successfully",
                admissionService.getMyLatestAdmission()
        );
    }

    @GetMapping("/me/details")
    public ApiResponse<StudentSectionAdmissionResponse> getMyDetailedAdmission() {
        return ApiResponse.success("Detailed admission retrieved successfully", admissionService.getMyDetailedAdmission());
    }

    @PutMapping("/me/details")
    public ApiResponse<StudentSectionAdmissionResponse> submitMyDetailedAdmission(
            @Valid @RequestBody DetailedAdmissionRequest request) {
        return ApiResponse.success("Detailed admission submitted to Student Section",
                admissionService.submitMyDetailedAdmission(request));
    }

    @PostMapping(path = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadMyPhoto(@RequestParam("file") MultipartFile file) {
        Long id = admissionService.getMyDetailedAdmission().id();
        photoService.save(id, file);
        return ApiResponse.success("Passport photo uploaded successfully", admissionService.getMyDetailedAdmission());
    }

    @GetMapping("/me/photo")
    public ResponseEntity<Resource> getMyPhoto() {
        Long id = admissionService.getMyDetailedAdmission().id();
        var photo = photoService.load(id);
        return ResponseEntity.ok().contentType(photo.mediaType()).body(photo.resource());
    }

    @PostMapping(path = "/me/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentSectionAdmissionResponse> uploadMyDocument(
            @PathVariable DocumentType type, @RequestParam("file") MultipartFile file) {
        Long id = admissionService.getMyDetailedAdmission().id();
        documentService.save(id, type, file);
        return ApiResponse.success("Admission document uploaded successfully", admissionService.getMyDetailedAdmission());
    }

    @GetMapping("/me/documents/{type}")
    public ResponseEntity<Resource> getMyDocument(@PathVariable DocumentType type) {
        Long id = admissionService.getMyDetailedAdmission().id();
        var document = documentService.load(id, type);
        return ResponseEntity.ok().contentType(document.mediaType()).body(document.resource());
    }
}
