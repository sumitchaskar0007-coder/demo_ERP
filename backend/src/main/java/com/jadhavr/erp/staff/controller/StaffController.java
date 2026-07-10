package com.jadhavr.erp.staff.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.staff.dto.CreateStudentSectionStaffRequest;
import com.jadhavr.erp.staff.dto.CreateFeeSectionStaffRequest;
import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/principal/staff")
public class StaffController {
    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping("/fee-section")
    public ResponseEntity<ApiResponse<StaffResponse>> createFeeSectionStaff(@Valid @RequestBody CreateFeeSectionStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Fee Section staff created successfully", staffService.createFeeSectionStaff(request)));
    }

    @PostMapping("/student-section")
    public ResponseEntity<ApiResponse<StaffResponse>> createStudentSectionStaff(
            @Valid @RequestBody CreateStudentSectionStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Student Section staff created successfully",
                        staffService.createStudentSectionStaff(request)
                ));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<StaffResponse>> searchStaff(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) StaffType staffType,
            @RequestParam(required = false) StaffStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Staff searched successfully",
                staffService.searchStaff(keyword, collegeId, staffType, status, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<StaffResponse> getStaffById(@PathVariable Long id) {
        return ApiResponse.success("Staff retrieved successfully", staffService.getStaffById(id));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<StaffResponse> activateStaff(@PathVariable Long id) {
        return ApiResponse.success("Staff activated successfully", staffService.activateStaff(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<StaffResponse> deactivateStaff(@PathVariable Long id) {
        return ApiResponse.success("Staff deactivated successfully", staffService.deactivateStaff(id));
    }
}
