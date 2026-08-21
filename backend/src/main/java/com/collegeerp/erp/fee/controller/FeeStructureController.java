package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.common.dto.PageResponse;
import com.collegeerp.erp.fee.dto.CreateFeeStructureRequest;
import com.collegeerp.erp.fee.dto.FeeStructureResponse;
import com.collegeerp.erp.fee.dto.UpdateFeeStructureRequest;
import com.collegeerp.erp.fee.enums.FeeStructureStatus;
import com.collegeerp.erp.fee.enums.StudentCategory;
import com.collegeerp.erp.fee.service.FeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/principal/fee-structures")
public class FeeStructureController {
    private final FeeService service;

    public FeeStructureController(FeeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FeeStructureResponse>> create(
            @Valid @RequestBody CreateFeeStructureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fee structure created successfully",
                        service.createStructure(request)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<FeeStructureResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
            @RequestParam(required = false) FeeStructureStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success("Fee structures retrieved", service.searchStructures(
                keyword, collegeId, departmentId, academicYear, studentCategory,
                status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeeStructureResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Fee structure retrieved", service.getStructure(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<FeeStructureResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateFeeStructureRequest request) {
        return ApiResponse.success("Fee structure updated", service.updateStructure(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteStructure(id);
        return ApiResponse.success("Fee structure deleted", null);
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<FeeStructureResponse> activate(@PathVariable Long id) {
        return ApiResponse.success("Fee structure activated",
                service.setStructureStatus(id, FeeStructureStatus.ACTIVE));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<FeeStructureResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success("Fee structure deactivated",
                service.setStructureStatus(id, FeeStructureStatus.INACTIVE));
    }

}
