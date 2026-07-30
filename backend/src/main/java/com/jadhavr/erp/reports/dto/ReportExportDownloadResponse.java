package com.jadhavr.erp.reports.dto;

import java.time.Instant;

public record ReportExportDownloadResponse(String url, Instant expiresAt) {}
