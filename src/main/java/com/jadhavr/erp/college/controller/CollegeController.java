package com.jadhavr.erp.college.controller;

import com.jadhavr.erp.college.dto.CollegeResponse;
import com.jadhavr.erp.college.dto.CreateCollegeRequest;
import com.jadhavr.erp.college.dto.UpdateCollegeRequest;
import com.jadhavr.erp.college.service.CollegeService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/colleges")
public class CollegeController {

    private final CollegeService collegeService;

    public CollegeController(CollegeService collegeService) {
        this.collegeService = collegeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollegeResponse>> createCollege(
            @Valid @RequestBody CreateCollegeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "College created successfully",
                        collegeService.createCollege(request)
                ));
    }

    @GetMapping
    public ApiResponse<List<CollegeResponse>> getAllColleges() {
        return ApiResponse.success("Colleges retrieved successfully", collegeService.getAllColleges());
    }

    @GetMapping("/active")
    public ApiResponse<List<CollegeResponse>> getActiveColleges() {
        return ApiResponse.success(
                "Active colleges retrieved successfully",
                collegeService.getActiveColleges()
        );
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<CollegeResponse>> searchColleges(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CollegeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Colleges searched successfully",
                collegeService.searchColleges(keyword, status, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<CollegeResponse> getCollegeById(@PathVariable Long id) {
        return ApiResponse.success("College retrieved successfully", collegeService.getCollegeById(id));
    }

    @GetMapping("/code/{code}")
    public ApiResponse<CollegeResponse> getCollegeByCode(@PathVariable String code) {
        return ApiResponse.success(
                "College retrieved successfully",
                collegeService.getCollegeByCode(code)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<CollegeResponse> updateCollege(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollegeRequest request) {
        return ApiResponse.success(
                "College updated successfully",
                collegeService.updateCollege(id, request)
        );
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<CollegeResponse> activateCollege(@PathVariable Long id) {
        return ApiResponse.success(
                "College activated successfully",
                collegeService.activateCollege(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<CollegeResponse> deactivateCollege(@PathVariable Long id) {
        return ApiResponse.success(
                "College deactivated successfully",
                collegeService.deactivateCollege(id)
        );
    }
}
