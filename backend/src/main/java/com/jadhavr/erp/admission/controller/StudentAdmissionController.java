package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.DetailedAdmissionRequest;
import com.jadhavr.erp.admission.dto.StudentAdmissionAccessResponse;
import com.jadhavr.erp.admission.dto.StudentSectionAdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionPhotoService;
import com.jadhavr.erp.admission.service.AdmissionService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    public StudentAdmissionController(AdmissionService admissionService, AdmissionPhotoService photoService) {
        this.admissionService = admissionService;
        this.photoService = photoService;
    }

    @GetMapping("/me")
    public ApiResponse<StudentSectionAdmissionResponse> getMyLatestAdmission() {
        return ApiResponse.success(
                "Admission retrieved successfully",
                admissionService.getMyDetailedAdmission()
        );
    }

    @GetMapping("/access-state")
    public ApiResponse<StudentAdmissionAccessResponse> accessState() {
        return ApiResponse.success("Student admission access retrieved", admissionService.getMyAdmissionAccess());
    }

    @PutMapping("/me/details")
    public ApiResponse<StudentSectionAdmissionResponse> submitDetails(
            @Valid @RequestBody DetailedAdmissionRequest request) {
        return ApiResponse.success(
                "Admission form submitted successfully and is pending review",
                admissionService.submitMyAdmissionDetails(request));
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
}
