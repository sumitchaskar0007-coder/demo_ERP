package com.jadhavr.erp.reports.controller;

import com.jadhavr.erp.academic.repository.AttendanceRecordRepository;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.reports.dto.AttendanceReportRow;
import com.jadhavr.erp.reports.service.AdmissionAnalyticsService;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/reports")
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','STUDENT_SECTION','FEE_SECTION')")
public class ReportController {
    private static final int MAX_PAGE_SIZE = 500;
    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final Set<String> EXPORT_TYPES = Set.of("admissions", "fees", "attendance", "students");

    private final AdmissionFormRepository admissions;
    private final StudentFeeAccountRepository fees;
    private final StudentProfileRepository students;
    private final AttendanceRecordRepository attendance;
    private final StaffProfileRepository staff;
    private final AuditLogService audit;
    private final AdmissionAnalyticsService admissionAnalytics;

    public ReportController(
            AdmissionFormRepository admissions,
            StudentFeeAccountRepository fees,
            StudentProfileRepository students,
            AttendanceRecordRepository attendance,
            StaffProfileRepository staff,
            AuditLogService audit,
            AdmissionAnalyticsService admissionAnalytics) {
        this.admissions = admissions;
        this.fees = fees;
        this.students = students;
        this.attendance = attendance;
        this.staff = staff;
        this.audit = audit;
        this.admissionAnalytics = admissionAnalytics;
    }

    @GetMapping("/admissions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','STUDENT_SECTION')")
    public ApiResponse<?> admissions(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        ReportScope scope = scope(collegeId, departmentId);
        Specification<AdmissionForm> spec = admissionSpec(scope, status);
        var rows = admissions.findAll(spec, PageRequest.of(validPage(page), validSize(size),
                        Sort.by(Sort.Direction.DESC, "submittedAt")))
                .getContent().stream().map(this::admissionRow).toList();
        viewed("Admission");
        return ApiResponse.success("Admission report", rows);
    }

    @GetMapping("/admissions/analytics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','STUDENT_SECTION')")
    public ApiResponse<?> admissionAnalytics(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String admissionNumber,
            @RequestParam(required = false) String mobile,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        ReportScope scope = scope(collegeId, departmentId);
        var analytics = admissionAnalytics.analytics(
                scope.collegeId(), scope.departmentId(), academicYear, status, from, to,
                studentName, admissionNumber, mobile, page, size, sort);
        viewed("Admission Analytics");
        return ApiResponse.success("Admission analytics", analytics);
    }

    @GetMapping("/fees")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','FEE_SECTION')")
    public ApiResponse<?> fees(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        ReportScope scope = scope(collegeId, departmentId);
        var rows = fees.findAll(feeSpec(scope), PageRequest.of(validPage(page), validSize(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(this::feeRow).toList();
        viewed("Fee");
        return ApiResponse.success("Fee report", rows);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD','STUDENT_SECTION')")
    public ApiResponse<?> students(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        ReportScope scope = scope(collegeId, departmentId);
        var rows = students.findAll(studentSpec(scope, status), PageRequest.of(validPage(page), validSize(size),
                        Sort.by(Sort.Direction.ASC, "fullName")))
                .getContent().stream().map(this::studentRow).toList();
        viewed("Student");
        return ApiResponse.success("Student report", rows);
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PRINCIPAL','HOD')")
    public ApiResponse<?> attendance(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        ReportScope scope = scope(collegeId, departmentId);
        var rows = attendance.attendanceReport(scope.collegeId(), scope.departmentId(),
                PageRequest.of(validPage(page), validSize(size)));
        viewed("Attendance");
        return ApiResponse.success("Attendance report", rows);
    }

    @GetMapping(value = "/{type:admissions|fees|attendance|students}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status) {
        authorizeExport(type);
        ReportScope scope = scope(collegeId, departmentId);
        String csv = switch (type) {
            case "admissions" -> exportAdmissions(scope, status);
            case "fees" -> exportFees(scope);
            case "attendance" -> exportAttendance(scope);
            case "students" -> exportStudents(scope, status);
            default -> throw new BadRequestException("Unsupported report type");
        };
        audit.logReportExport(type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + "-report.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private String exportAdmissions(ReportScope scope, String status) {
        List<AdmissionForm> rows = admissions.findAll(admissionSpec(scope, status), exportPage()).getContent();
        requireWithinLimit(rows.size(), "admission");
        StringBuilder csv = new StringBuilder("Reference,Admission Number,Student,College,Department,Status\n");
        rows.forEach(row -> csv.append(cell(row.getAdmissionReferenceNumber())).append(',')
                .append(cell(row.getStudent().getAdmissionNumber())).append(',')
                .append(cell(row.getFullName())).append(',')
                .append(cell(row.getCollege().getName())).append(',')
                .append(cell(row.getDepartment().getName())).append(',')
                .append(cell(row.getStatus().name())).append('\n'));
        return csv.toString();
    }

    private String exportFees(ReportScope scope) {
        List<StudentFeeAccount> rows = fees.findAll(feeSpec(scope), exportPage()).getContent();
        requireWithinLimit(rows.size(), "fee");
        StringBuilder csv = new StringBuilder("Student,Admission Number,Total,Paid,Remaining,Status\n");
        rows.forEach(row -> csv.append(cell(row.getStudent().getFullName())).append(',')
                .append(cell(row.getStudent().getAdmissionNumber())).append(',')
                .append(cell(row.getTotalFee())).append(',')
                .append(cell(row.getPaidAmount())).append(',')
                .append(cell(row.getRemainingAmount())).append(',')
                .append(cell(row.getStatus().name())).append('\n'));
        return csv.toString();
    }

    private String exportAttendance(ReportScope scope) {
        List<AttendanceReportRow> rows = attendance.attendanceReport(
                scope.collegeId(), scope.departmentId(), exportPage());
        requireWithinLimit(rows.size(), "attendance");
        StringBuilder csv = new StringBuilder(
                "Student,Roll Number,Total Sessions,Present,Absent,Attendance Percentage\n");
        rows.forEach(row -> csv.append(cell(row.getStudentName())).append(',')
                .append(cell(row.getRollNumber())).append(',')
                .append(cell(row.getTotalSessions())).append(',')
                .append(cell(row.getPresentCount())).append(',')
                .append(cell(row.getAbsentCount())).append(',')
                .append(cell(String.format(java.util.Locale.ROOT, "%.2f", row.getAttendancePercentage())))
                .append('\n'));
        return csv.toString();
    }

    private String exportStudents(ReportScope scope, String status) {
        List<StudentProfile> rows = students.findAll(studentSpec(scope, status), exportPage()).getContent();
        requireWithinLimit(rows.size(), "student");
        StringBuilder csv = new StringBuilder("Student,Roll Number,Email,Phone,College,Department,Status\n");
        rows.forEach(row -> csv.append(cell(row.getFullName())).append(',')
                .append(cell(row.getRollNumber())).append(',')
                .append(cell(row.getEmail())).append(',')
                .append(cell(row.getPhone())).append(',')
                .append(cell(row.getCollege().getName())).append(',')
                .append(cell(row.getDepartment().getName())).append(',')
                .append(cell(row.getStatus().name())).append('\n'));
        return csv.toString();
    }

    private ReportScope scope(Long requestedCollegeId, Long requestedDepartmentId) {
        if (SecurityUtils.isSuperAdmin()) return new ReportScope(requestedCollegeId, requestedDepartmentId);
        Long ownCollegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (ownCollegeId == null) throw new AccessDeniedException("College access is required");
        if (requestedCollegeId != null && !Objects.equals(requestedCollegeId, ownCollegeId)) {
            throw new AccessDeniedException("Report is outside your college");
        }
        if (!SecurityUtils.hasRole("HOD")) return new ReportScope(ownCollegeId, requestedDepartmentId);

        StaffProfile profile = staff.findByUserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AccessDeniedException("HOD profile is required"));
        Long ownDepartmentId = profile.getDepartment() == null ? null : profile.getDepartment().getId();
        if (ownDepartmentId == null) throw new AccessDeniedException("HOD department is required");
        if (requestedDepartmentId != null && !profile.belongsToDepartment(requestedDepartmentId)) {
            throw new AccessDeniedException("Report is outside your department");
        }
        return new ReportScope(ownCollegeId,
                requestedDepartmentId == null ? ownDepartmentId : requestedDepartmentId);
    }

    private void authorizeExport(String type) {
        if (!EXPORT_TYPES.contains(type)) throw new BadRequestException("Unsupported report type");
        boolean allowed = SecurityUtils.isSuperAdmin()
                || SecurityUtils.hasRole("PRINCIPAL")
                || ("fees".equals(type) && SecurityUtils.hasRole("FEE_SECTION"))
                || (Set.of("admissions", "students").contains(type) && SecurityUtils.hasRole("STUDENT_SECTION"))
                || (Set.of("admissions", "students", "attendance").contains(type) && SecurityUtils.hasRole("HOD"));
        if (!allowed) throw new AccessDeniedException("You are not allowed to export this report");
    }

    private Specification<AdmissionForm> admissionSpec(ReportScope scope, String status) {
        Specification<AdmissionForm> spec = Specification.where(null);
        if (scope.collegeId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("college").get("id"), scope.collegeId()));
        if (scope.departmentId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("department").get("id"), scope.departmentId()));
        if (status != null && !status.isBlank()) {
            AdmissionStatus value;
            try {
                value = AdmissionStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Invalid admission status");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), value));
        }
        return spec;
    }

    private Specification<StudentFeeAccount> feeSpec(ReportScope scope) {
        Specification<StudentFeeAccount> spec = Specification.where(null);
        if (scope.collegeId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("college").get("id"), scope.collegeId()));
        if (scope.departmentId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("department").get("id"), scope.departmentId()));
        return spec;
    }

    private Specification<StudentProfile> studentSpec(ReportScope scope, String status) {
        Specification<StudentProfile> spec = Specification.where(null);
        if (scope.collegeId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("college").get("id"), scope.collegeId()));
        if (scope.departmentId() != null) spec = spec.and((root, query, cb) ->
                cb.equal(root.get("department").get("id"), scope.departmentId()));
        if (status != null && !status.isBlank()) {
            StudentStatus value;
            try {
                value = StudentStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Invalid student status");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), value));
        }
        return spec;
    }

    private Map<String, Object> admissionRow(AdmissionForm row) {
        return Map.ofEntries(
                Map.entry("admissionReferenceNumber", row.getAdmissionReferenceNumber()),
                Map.entry("admissionNumber", row.getStudent().getAdmissionNumber()),
                Map.entry("studentName", row.getFullName()),
                Map.entry("email", row.getEmail()),
                Map.entry("phone", row.getPhone()),
                Map.entry("collegeName", row.getCollege().getName()),
                Map.entry("departmentName", row.getDepartment().getName()),
                Map.entry("academicYear", row.getAcademicYear()),
                Map.entry("status", row.getStatus()),
                Map.entry("submittedAt", row.getSubmittedAt()));
    }

    private Map<String, Object> feeRow(StudentFeeAccount row) {
        return Map.ofEntries(
                Map.entry("studentName", row.getStudent().getFullName()),
                Map.entry("admissionNumber", row.getStudent().getAdmissionNumber()),
                Map.entry("collegeName", row.getCollege().getName()),
                Map.entry("departmentName", row.getDepartment().getName()),
                Map.entry("totalFee", row.getTotalFee()),
                Map.entry("paidAmount", row.getPaidAmount()),
                Map.entry("remainingAmount", row.getRemainingAmount()),
                Map.entry("feeStatus", row.getStatus()));
    }

    private Map<String, Object> studentRow(StudentProfile row) {
        return Map.ofEntries(
                Map.entry("studentName", row.getFullName()),
                Map.entry("rollNumber", Objects.toString(row.getRollNumber(), "")),
                Map.entry("email", row.getEmail()),
                Map.entry("phone", row.getPhone()),
                Map.entry("collegeName", row.getCollege().getName()),
                Map.entry("departmentName", row.getDepartment().getName()),
                Map.entry("studentStatus", row.getStatus()));
    }

    private PageRequest exportPage() {
        return PageRequest.of(0, MAX_EXPORT_ROWS + 1, Sort.by(Sort.Direction.ASC, "id"));
    }

    private void requireWithinLimit(int rowCount, String reportName) {
        if (rowCount > MAX_EXPORT_ROWS) {
            throw new BadRequestException("The " + reportName
                    + " report exceeds 10,000 rows; narrow the filters or use a background export job");
        }
    }

    private int validPage(int page) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        return page;
    }

    private int validSize(int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return size;
    }

    private void viewed(String reportName) {
        audit.logReportView(reportName);
    }

    private String cell(Object value) {
        String text = Objects.toString(value, "");
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private record ReportScope(Long collegeId, Long departmentId) {}
}
