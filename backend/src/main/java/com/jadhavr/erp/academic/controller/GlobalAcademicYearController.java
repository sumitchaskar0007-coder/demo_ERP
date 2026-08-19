package com.jadhavr.erp.academic.controller;

import com.jadhavr.erp.academic.dto.GlobalAcademicYearDtos.SaveRequest;
import com.jadhavr.erp.academic.dto.GlobalAcademicYearDtos.View;
import com.jadhavr.erp.academic.service.GlobalAcademicYearService;
import com.jadhavr.erp.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class GlobalAcademicYearController {
    private final GlobalAcademicYearService service;

    public GlobalAcademicYearController(GlobalAcademicYearService service) {
        this.service = service;
    }

    @GetMapping("/api/global-academic-years/active")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<View> active() {
        return ApiResponse.success("Active global academic year", service.active());
    }

    @GetMapping("/api/super-admin/academic-years")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<View>> list() {
        return ApiResponse.success("Global academic years", service.list());
    }

    @PostMapping("/api/super-admin/academic-years")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<View> create(@Valid @RequestBody SaveRequest request) {
        return ApiResponse.success("Academic year created", service.create(request));
    }

    @PutMapping("/api/super-admin/academic-years/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<View> update(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        return ApiResponse.success("Academic year updated", service.update(id, request));
    }

    @PostMapping("/api/super-admin/academic-years/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<View> activate(@PathVariable Long id) {
        return ApiResponse.success("Academic year activated for all colleges", service.activate(id));
    }
}
