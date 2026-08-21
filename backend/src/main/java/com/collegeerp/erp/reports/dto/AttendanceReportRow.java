package com.collegeerp.erp.reports.dto;

public interface AttendanceReportRow {
    String getStudentName();
    String getRollNumber();
    long getTotalSessions();
    long getPresentCount();

    default long getAbsentCount() {
        return getTotalSessions() - getPresentCount();
    }

    default double getAttendancePercentage() {
        return getTotalSessions() == 0 ? 0 : getPresentCount() * 100.0 / getTotalSessions();
    }
}
