package com.jadhavr.erp.admission.controller;

import com.jadhavr.erp.admission.dto.PublicAdmissionInfoResponse;
import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.dto.SubmitAdmissionResponse;
import com.jadhavr.erp.admission.service.AdmissionService;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.college.service.CollegeImageStorageService;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.fee.dto.FeeCategoryOptionResponse;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeeStructureRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/admissions")
public class PublicAdmissionController {
    private static final java.util.Comparator<FeeStructure> CATEGORY_DISPLAY_ORDER =
            java.util.Comparator
                    .comparingInt((FeeStructure fee) -> fee.getStudentCategory().displayOrder())
                    .thenComparing(
                            fee -> fee.getCustomCategoryName() == null
                                    ? "" : fee.getCustomCategoryName(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(
                            FeeStructure::getId,
                            java.util.Comparator.nullsLast(Long::compareTo));

    private final AdmissionService admissionService;
    private final CollegeRepository colleges;
    private final CollegeImageStorageService images;
    private final FeeStructureRepository feeStructures;

    public PublicAdmissionController(AdmissionService admissionService, CollegeRepository colleges,
            CollegeImageStorageService images, FeeStructureRepository feeStructures) {
        this.admissionService = admissionService;
        this.colleges = colleges;
        this.images = images;
        this.feeStructures = feeStructures;
    }

    @GetMapping("/college/{collegeCode}/departments/{departmentId}/categories")
    public ApiResponse<java.util.List<FeeCategoryOptionResponse>> categories(
            @PathVariable String collegeCode,
            @PathVariable Long departmentId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String courseYear) {
        var college = colleges.findByCode(collegeCode.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
        String normalizedGender = gender == null || gender.isBlank() ? null
                : com.jadhavr.erp.fee.service.FeeCategoryRules.normalizeGender(gender);
        java.util.List<String> academicYears = academicYear == null || academicYear.isBlank() ? null
                : com.jadhavr.erp.fee.service.FeeCategoryRules.academicYearVariants(academicYear);
        String normalizedCourseYear = courseYear == null || courseYear.isBlank()
                ? null : courseYear.trim();
        var configured = feeStructures.findPublicCategoryOptions(college.getId(), departmentId)
                .stream()
                .filter(f -> f.getGender() != null)
                .filter(f -> normalizedGender == null || normalizedGender.equalsIgnoreCase(f.getGender()))
                .filter(f -> academicYears == null || academicYears.contains(f.getAcademicYear()))
                .filter(f -> normalizedCourseYear == null
                        || normalizedCourseYear.equalsIgnoreCase(f.getCourseYear()))
                .sorted(CATEGORY_DISPLAY_ORDER)
                .toList();
        var options = new java.util.LinkedHashMap<String, FeeCategoryOptionResponse>();
        configured.forEach(f -> {
            StudentCategory category = f.getStudentCategory();
            if (category != StudentCategory.OTHER) {
                options.putIfAbsent(category.name(),
                        new FeeCategoryOptionResponse(category, null, category.name()));
                return;
            }
            String custom = f.getCustomCategoryName();
            if (custom == null
                    || com.jadhavr.erp.fee.service.FeeCategoryRules.isReservedCustomCategory(custom)) {
                return;
            }
            options.putIfAbsent("OTHER:" + custom.toUpperCase(java.util.Locale.ROOT),
                    new FeeCategoryOptionResponse(StudentCategory.OTHER, custom, custom));
        });
        return ApiResponse.success("Admission categories retrieved", java.util.List.copyOf(options.values()));
    }

    @GetMapping("/college/{collegeCode}/logo")
    public ResponseEntity<org.springframework.core.io.Resource> collegeLogo(
            @PathVariable String collegeCode) {
        var college = colleges.findByCode(collegeCode.trim().toUpperCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
        if (college.getLogoUrl() == null) return ResponseEntity.notFound().build();
        var image = images.load(college.getLogoUrl());
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .header("Content-Disposition", "inline; filename=\"college-logo\"")
                .body(image.resource());
    }

    @GetMapping("/college/{collegeCode}/info")
    public ApiResponse<PublicAdmissionInfoResponse> getPublicAdmissionInfo(
            @PathVariable String collegeCode) {
        return ApiResponse.success(
                "Public admission info retrieved successfully",
                admissionService.getPublicAdmissionInfo(collegeCode)
        );
    }

    @PostMapping("/college/{collegeCode}/submit")
    public ResponseEntity<ApiResponse<SubmitAdmissionResponse>> submitAdmission(
            @PathVariable String collegeCode,
            @Valid @RequestBody SubmitAdmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Admission submitted successfully",
                        admissionService.submitAdmission(collegeCode, request)
                ));
    }
}
