package com.jadhavr.erp.reports.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.reports.dto.CreateReportExportRequest;
import com.jadhavr.erp.reports.dto.ReportExportDownloadResponse;
import com.jadhavr.erp.reports.dto.ReportExportJobResponse;
import com.jadhavr.erp.reports.service.ReportExportJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','STUDENT_SECTION','FEE_SECTION')")
public class ReportExportJobController {
    private final ReportExportJobService jobs;

    public ReportExportJobController(ReportExportJobService jobs) {
        this.jobs = jobs;
    }

    @PostMapping("/{type:admissions|fees|attendance|students}/jobs")
    public ResponseEntity<ApiResponse<ReportExportJobResponse>> create(
            @PathVariable String type,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReportExportRequest request) {
        ReportExportJobService.CreateResult result = jobs.create(type, idempotencyKey, request);
        HttpStatus status = result.reused() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        String message = result.reused()
                ? "Existing report export job"
                : "Report export job queued";
        return ResponseEntity.status(status).body(ApiResponse.success(message, result.job()));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<ReportExportJobResponse> status(@PathVariable UUID jobId) {
        return ApiResponse.success("Report export job", jobs.status(jobId));
    }

    @GetMapping("/jobs/{jobId}/download")
    public ApiResponse<ReportExportDownloadResponse> download(@PathVariable UUID jobId) {
        return ApiResponse.success("Private report download", jobs.download(jobId));
    }
}
