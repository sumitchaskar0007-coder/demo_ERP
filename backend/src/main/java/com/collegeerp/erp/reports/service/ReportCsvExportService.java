package com.collegeerp.erp.reports.service;

import com.collegeerp.erp.academic.repository.AttendanceRecordRepository;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.reports.config.ReportExportProperties;
import com.collegeerp.erp.reports.dto.AdmissionCsvRow;
import com.collegeerp.erp.reports.dto.AttendanceReportRow;
import com.collegeerp.erp.reports.dto.FeeCsvRow;
import com.collegeerp.erp.reports.dto.StudentCsvRow;
import com.collegeerp.erp.reports.enums.ReportExportType;
import com.collegeerp.erp.reports.repository.ReportExportReadRepository;
import com.collegeerp.erp.student.enums.StudentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class ReportCsvExportService {
    public static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final ReportExportReadRepository rows;
    private final AttendanceRecordRepository attendance;
    private final ReportExportProperties properties;

    public ReportCsvExportService(
            ReportExportReadRepository rows,
            AttendanceRecordRepository attendance,
            ReportExportProperties properties) {
        this.rows = rows;
        this.attendance = attendance;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public GeneratedReport generate(
            ReportExportType type, Long collegeId, Long departmentId, String statusFilter) {
        if (collegeId == null) throw new BadRequestException("A college-scoped report is required");
        CsvBuffer csv = new CsvBuffer(properties.getMaxOutputBytes());
        int rowCount = switch (type) {
            case ADMISSIONS -> admissions(csv, collegeId, departmentId, statusFilter);
            case FEES -> fees(csv, collegeId, departmentId);
            case ATTENDANCE -> attendance(csv, collegeId, departmentId);
            case STUDENTS -> students(csv, collegeId, departmentId, statusFilter);
        };
        return new GeneratedReport(csv.bytes(), rowCount);
    }

    public String validateStatusFilter(ReportExportType type, String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (type) {
                case ADMISSIONS -> AdmissionStatus.valueOf(normalized).name();
                case STUDENTS -> StudentStatus.valueOf(normalized).name();
                case FEES, ATTENDANCE ->
                        throw new BadRequestException("status is not supported for this report type");
            };
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid report status");
        }
    }

    private int admissions(CsvBuffer csv, Long collegeId, Long departmentId, String status) {
        csv.write(CsvCell.row(
                "Reference", "Admission Number", "Student", "College", "Department", "Status"));
        int count = 0;
        long afterId = 0;
        List<AdmissionCsvRow> page;
        AdmissionStatus parsedStatus =
                status == null ? null : AdmissionStatus.valueOf(status);
        do {
            page = rows.admissions(
                    collegeId,
                    departmentId,
                    parsedStatus,
                    afterId,
                    properties.getPageSize());
            for (AdmissionCsvRow row : page) {
                requireCapacity(++count);
                csv.write(CsvCell.row(
                        row.reference(),
                        row.admissionNumber(),
                        row.studentName(),
                        row.collegeName(),
                        row.departmentName(),
                        row.status().name()));
                afterId = row.id();
            }
        } while (page.size() == properties.getPageSize());
        return count;
    }

    private int fees(CsvBuffer csv, Long collegeId, Long departmentId) {
        csv.write(CsvCell.row(
                "Student", "Admission Number", "Total", "Paid", "Remaining", "Status"));
        int count = 0;
        long afterId = 0;
        List<FeeCsvRow> page;
        do {
            page = rows.fees(
                    collegeId, departmentId, afterId, properties.getPageSize());
            for (FeeCsvRow row : page) {
                requireCapacity(++count);
                csv.write(CsvCell.row(
                        row.studentName(),
                        row.admissionNumber(),
                        row.total(),
                        row.paid(),
                        row.remaining(),
                        row.status().name()));
                afterId = row.id();
            }
        } while (page.size() == properties.getPageSize());
        return count;
    }

    private int students(CsvBuffer csv, Long collegeId, Long departmentId, String status) {
        csv.write(CsvCell.row(
                "Student", "Roll Number", "Email", "Phone", "College", "Department", "Status"));
        int count = 0;
        long afterId = 0;
        List<StudentCsvRow> page;
        StudentStatus parsedStatus = status == null ? null : StudentStatus.valueOf(status);
        do {
            page = rows.students(
                    collegeId,
                    departmentId,
                    parsedStatus,
                    afterId,
                    properties.getPageSize());
            for (StudentCsvRow row : page) {
                requireCapacity(++count);
                csv.write(CsvCell.row(
                        row.studentName(),
                        row.rollNumber(),
                        row.email(),
                        row.phone(),
                        row.collegeName(),
                        row.departmentName(),
                        row.status().name()));
                afterId = row.id();
            }
        } while (page.size() == properties.getPageSize());
        return count;
    }

    private int attendance(CsvBuffer csv, Long collegeId, Long departmentId) {
        csv.write(CsvCell.row(
                "Student",
                "Roll Number",
                "Total Sessions",
                "Present",
                "Absent",
                "Attendance Percentage"));
        int count = 0;
        int pageNumber = 0;
        List<AttendanceReportRow> rows;
        do {
            rows = attendance.attendanceReport(
                    collegeId,
                    departmentId,
                    PageRequest.of(pageNumber++, properties.getPageSize()));
            for (AttendanceReportRow row : rows) {
                requireCapacity(++count);
                csv.write(CsvCell.row(
                        row.getStudentName(),
                        row.getRollNumber(),
                        row.getTotalSessions(),
                        row.getPresentCount(),
                        row.getAbsentCount(),
                        String.format(
                                Locale.ROOT, "%.2f", row.getAttendancePercentage())));
            }
        } while (rows.size() == properties.getPageSize());
        return count;
    }

    private void requireCapacity(int count) {
        if (count > properties.getMaxRows()) {
            throw new BadRequestException(
                    "The report exceeds the configured asynchronous export row limit");
        }
    }

    public record GeneratedReport(byte[] content, int rowCount) {}

    private static final class CsvBuffer {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final long maximumBytes;

        private CsvBuffer(long maximumBytes) {
            this.maximumBytes = maximumBytes;
        }

        private void write(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            if ((long) output.size() + bytes.length > maximumBytes) {
                throw new BadRequestException(
                        "The report exceeds the configured asynchronous export size limit");
            }
            output.writeBytes(bytes);
        }

        private byte[] bytes() {
            return output.toByteArray();
        }
    }
}
