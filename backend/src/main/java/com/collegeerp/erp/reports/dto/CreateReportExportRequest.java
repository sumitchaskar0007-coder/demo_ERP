package com.collegeerp.erp.reports.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReportExportRequest(
        @Positive Long collegeId,
        @Positive Long departmentId,
        @Size(max = 40) String status) {}
