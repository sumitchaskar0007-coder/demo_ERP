package com.jadhavr.erp.student.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.student.dto.StudentProfileResponse;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.service.AdminStudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/super-admin/students", "/api/principal/students"})
public class AdminStudentController {
    private final AdminStudentService adminStudentService;
    private final StudentProfileRepository students;
    private final AdmissionFormRepository admissions;
    private final StudentSectionEnrollmentRepository enrollments;
    private final WeeklyAttendanceRecordRepository attendance;
    private final StudentFeeAccountRepository feeAccounts;

    public AdminStudentController(AdminStudentService adminStudentService, StudentProfileRepository students,
            AdmissionFormRepository admissions, StudentSectionEnrollmentRepository enrollments,
            WeeklyAttendanceRecordRepository attendance,
            StudentFeeAccountRepository feeAccounts) {
        this.adminStudentService = adminStudentService;
        this.students = students;
        this.admissions = admissions;
        this.enrollments = enrollments;
        this.attendance = attendance;
        this.feeAccounts = feeAccounts;
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<StudentProfileResponse>> searchStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(
                "Students searched successfully",
                adminStudentService.searchStudents(
                        keyword, collegeId, departmentId, status, page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentProfileResponse> getStudent(@PathVariable Long id) {
        return ApiResponse.success("Student retrieved successfully", adminStudentService.getStudentById(id));
    }

    @GetMapping("/{id}/details")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getStudentDetails(@PathVariable Long id) {
        var student = students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", adminStudentService.getStudentById(id));
        feeAccounts.findTopByStudentIdOrderByCreatedAtDesc(id).ifPresent(account ->
                result.put("fees", Map.of(
                        "feeAccountId", account.getId(),
                        "totalFee", account.getTotalFee(),
                        "paidAmount", account.getPaidAmount(),
                        "scholarshipAmount", account.getDiscountAmount(),
                        "remainingAmount", account.getRemainingAmount(),
                        "minimumAmountForAdmission", account.getMinimumAmountForAdmission(),
                        "status", account.getStatus())));
        admissions.findTopByStudentIdOrderByCreatedAtDesc(id).ifPresent(a -> {
            Map<String, Object> admission = new LinkedHashMap<>();
            admission.put("id", a.getId());
            admission.put("referenceNumber", a.getAdmissionReferenceNumber());
            admission.put("academicYear", a.getAcademicYear());
            admission.put("status", a.getStatus());
            admission.put("submittedAt", a.getSubmittedAt());
            result.put("admission", admission);
        });
        enrollments.findFirstByStudentAndStatus(student, AcademicStatus.ACTIVE).ifPresent(e -> {
            result.put("academic", Map.of("courseYear", e.getAcademicClass().getName(),
                    "division", e.getSection().getName(), "academicYear", e.getAcademicYear(),
                    "rollNumber", e.getRollNumber()));
        });
        var records = attendance.findByStudentIdOrderBySessionAttendanceDateDescSessionStartTimeDesc(id);
        long present = records.stream().filter(r -> r.getStatus().name().equals("PRESENT")).count();
        long late = records.stream().filter(r -> r.getStatus().name().equals("LATE")).count();
        long leave = records.stream().filter(r -> r.getStatus().name().equals("LEAVE")).count();
        double percentage = records.isEmpty() ? 0 : Math.round(((present + late) * 10000.0) / records.size()) / 100.0;
        result.put("attendance", Map.of("totalLectures", records.size(), "present", present,
                "absent", records.size() - present - late - leave, "late", late, "leave", leave,
                "percentage", percentage));
        return ApiResponse.success("Student details retrieved", result);
    }
}
