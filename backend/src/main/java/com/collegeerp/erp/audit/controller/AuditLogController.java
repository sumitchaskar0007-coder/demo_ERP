package com.collegeerp.erp.audit.controller;

import com.collegeerp.erp.audit.enums.AuditAction;
import com.collegeerp.erp.audit.enums.AuditModule;
import com.collegeerp.erp.audit.service.AuditLogService;
import com.collegeerp.erp.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL')")
public class AuditLogController {
    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) AuditModule module,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Audit logs", service.search(
                keyword, collegeId, module, action, actorUserId, dateFrom, dateTo, page, size));
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) AuditModule module,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        return ApiResponse.success("Business activity dashboard", service.dashboard(
                academicYear, departmentId, module, role, userId, action,
                dateFrom, dateTo, search, page, size, sort));
    }
}
