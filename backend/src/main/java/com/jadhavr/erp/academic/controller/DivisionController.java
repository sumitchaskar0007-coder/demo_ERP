package com.jadhavr.erp.academic.controller;

import com.jadhavr.erp.academic.dto.AssignClassTeacherRequest;
import com.jadhavr.erp.academic.dto.CreateDivisionRequest;
import com.jadhavr.erp.academic.dto.DivisionResponse;
import com.jadhavr.erp.academic.dto.UpdateDivisionRequest;
import com.jadhavr.erp.academic.enums.SectionStatus;
import com.jadhavr.erp.academic.service.DivisionService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.staff.dto.StaffResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/principal/divisions")
public class DivisionController {
    private final DivisionService service;

    public DivisionController(DivisionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DivisionResponse>> create(@Valid @RequestBody CreateDivisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Division created", service.create(request)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<DivisionResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) SectionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Divisions retrieved", service.search(
                keyword, departmentId, courseYearId, academicYear, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DivisionResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Division retrieved", service.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DivisionResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateDivisionRequest request) {
        return ApiResponse.success("Division updated", service.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<DivisionResponse> activate(@PathVariable Long id) {
        return ApiResponse.success("Division activated", service.setStatus(id, SectionStatus.ACTIVE));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<DivisionResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success("Division deactivated", service.setStatus(id, SectionStatus.INACTIVE));
    }

    @PatchMapping("/{id}/assign-class-teacher")
    public ApiResponse<DivisionResponse> assign(@PathVariable Long id,
            @Valid @RequestBody AssignClassTeacherRequest request) {
        return ApiResponse.success("Class Teacher assigned", service.assignClassTeacher(id, request));
    }

    @PatchMapping("/{id}/remove-class-teacher")
    public ApiResponse<DivisionResponse> remove(@PathVariable Long id) {
        return ApiResponse.success("Class Teacher removed", service.removeClassTeacher(id));
    }

    @GetMapping("/{id}/eligible-class-teachers")
    public ApiResponse<List<StaffResponse>> eligible(@PathVariable Long id) {
        return ApiResponse.success("Eligible Class Teachers retrieved", service.eligibleClassTeachers(id));
    }
}
