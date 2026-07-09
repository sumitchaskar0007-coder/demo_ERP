package com.jadhavr.erp.department.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.department.dto.CreateDepartmentRequest;
import com.jadhavr.erp.department.dto.DepartmentResponse;
import com.jadhavr.erp.department.dto.UpdateDepartmentRequest;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.service.DepartmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/principal/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Department created successfully",
                departmentService.createDepartment(request)
        ));
    }

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> getAllDepartments() {
        return ApiResponse.success(
                "Departments retrieved successfully",
                departmentService.getAllDepartments()
        );
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<DepartmentResponse>> searchDepartments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) DepartmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Departments searched successfully",
                departmentService.searchDepartments(
                        keyword, collegeId, status, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ApiResponse.success(
                "Department retrieved successfully",
                departmentService.getDepartmentById(id)
        );
    }

    @GetMapping("/college/{collegeId}/code/{code}")
    public ApiResponse<DepartmentResponse> getDepartmentByCollegeAndCode(
            @PathVariable Long collegeId, @PathVariable String code) {
        return ApiResponse.success(
                "Department retrieved successfully",
                departmentService.getDepartmentByCollegeIdAndCode(collegeId, code)
        );
    }

    @GetMapping("/college/{collegeId}")
    public ApiResponse<List<DepartmentResponse>> getDepartmentsByCollege(
            @PathVariable Long collegeId) {
        return ApiResponse.success(
                "College departments retrieved successfully",
                departmentService.getDepartmentsByCollege(collegeId)
        );
    }

    @GetMapping("/college/{collegeId}/active")
    public ApiResponse<List<DepartmentResponse>> getActiveDepartmentsByCollege(
            @PathVariable Long collegeId) {
        return ApiResponse.success(
                "Active college departments retrieved successfully",
                departmentService.getActiveDepartmentsByCollege(collegeId)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success(
                "Department updated successfully",
                departmentService.updateDepartment(id, request)
        );
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<DepartmentResponse> activateDepartment(@PathVariable Long id) {
        return ApiResponse.success(
                "Department activated successfully",
                departmentService.activateDepartment(id)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<DepartmentResponse> deactivateDepartment(@PathVariable Long id) {
        return ApiResponse.success(
                "Department deactivated successfully",
                departmentService.deactivateDepartment(id)
        );
    }
}
