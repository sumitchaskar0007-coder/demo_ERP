package com.collegeerp.erp.reports.enums;

import com.collegeerp.erp.common.exception.BadRequestException;

import java.util.Locale;

public enum ReportExportType {
    ADMISSIONS,
    FEES,
    ATTENDANCE,
    STUDENTS;

    public static ReportExportType fromPath(String value) {
        if (value == null) throw new BadRequestException("Report type is required");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported report type");
        }
    }

    public String pathValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
