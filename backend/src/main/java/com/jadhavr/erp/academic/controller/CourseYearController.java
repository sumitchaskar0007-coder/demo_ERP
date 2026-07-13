package com.jadhavr.erp.academic.controller;

import com.jadhavr.erp.academic.dto.CreateCourseYearRequest;
import com.jadhavr.erp.academic.dto.CourseYearResponse;
import com.jadhavr.erp.academic.dto.UpdateCourseYearRequest;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.service.CourseYearService;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/principal/course-years")
public class CourseYearController {
    private final CourseYearService service;

    public CourseYearController(CourseYearService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseYearResponse>> create(@Valid @RequestBody CreateCourseYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course Year created", service.create(request)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<CourseYearResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) AcademicStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success("Course Years retrieved", service.search(
                keyword, departmentId, academicYear, status, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseYearResponse> get(@PathVariable Long id) {
        return ApiResponse.success("Course Year retrieved", service.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseYearResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateCourseYearRequest request) {
        return ApiResponse.success("Course Year updated", service.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<CourseYearResponse> activate(@PathVariable Long id) {
        return ApiResponse.success("Course Year activated", service.setStatus(id, AcademicStatus.ACTIVE));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<CourseYearResponse> deactivate(@PathVariable Long id) {
        return ApiResponse.success("Course Year deactivated", service.setStatus(id, AcademicStatus.INACTIVE));
    }
}
