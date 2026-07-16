package com.jadhavr.erp.analytics.controller;

import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.AttendanceSessionRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.SubjectRepository;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.fee.dto.FeeBalanceTotals;
import com.jadhavr.erp.fee.enums.PaymentStatus;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final CollegeRepository colleges;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final StudentProfileRepository students;
    private final StaffProfileRepository staff;
    private final AdmissionFormRepository admissions;
    private final StudentFeeAccountRepository fees;
    private final FeePaymentRepository payments;
    private final AcademicClassRepository classes;
    private final SectionRepository sections;
    private final SubjectRepository subjects;
    private final AttendanceSessionRepository attendance;

    public DashboardController(CollegeRepository colleges, DepartmentRepository departments,
            UserRepository users, StudentProfileRepository students, StaffProfileRepository staff,
            AdmissionFormRepository admissions, StudentFeeAccountRepository fees,
            FeePaymentRepository payments, AcademicClassRepository classes,
            SectionRepository sections, SubjectRepository subjects,
            AttendanceSessionRepository attendance) {
        this.colleges = colleges;
        this.departments = departments;
        this.users = users;
        this.students = students;
        this.staff = staff;
        this.admissions = admissions;
        this.fees = fees;
        this.payments = payments;
        this.classes = classes;
        this.sections = sections;
        this.subjects = subjects;
        this.attendance = attendance;
    }

    @GetMapping("/super-admin")
    public ApiResponse<?> superAdmin() {
        FeeBalanceTotals totals = fees.balanceTotals();
        return ApiResponse.success("Super Admin dashboard", Map.ofEntries(
                Map.entry("totalColleges", colleges.count()),
                Map.entry("totalDepartments", departments.count()),
                Map.entry("totalUsers", users.count()),
                Map.entry("totalStudents", students.count()),
                Map.entry("totalAdmissions", admissions.count()),
                Map.entry("totalFeeCollected", totals.totalPaid()),
                Map.entry("totalFeePending", totals.totalRemaining()),
                Map.entry("totalAttendanceSessions", attendance.count())));
    }

    @GetMapping("/principal")
    public ApiResponse<?> principal(@RequestParam(required = false) Long collegeId) {
        return ApiResponse.success("Principal dashboard", college(scopeCollege(collegeId)));
    }

    @GetMapping("/student-section")
    public ApiResponse<?> studentSection() {
        Long collegeId = scopeCollege(null);
        return ApiResponse.success("Student Section dashboard", Map.of(
                "submittedAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.SUBMITTED),
                "reviewPendingAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING),
                "approvedByStudentSection", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.STUDENT_SECTION_APPROVED),
                "rejectedByStudentSection", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.STUDENT_SECTION_REJECTED)));
    }

    @GetMapping("/fee-section")
    public ApiResponse<?> feeSection() {
        Long collegeId = scopeCollege(null);
        FeeBalanceTotals totals = fees.balanceTotalsByCollegeId(collegeId);
        return ApiResponse.success("Fee Section dashboard", Map.of(
                "totalFeeAccounts", fees.countByCollegeId(collegeId),
                "pendingPayments", payments.countByCollegeIdAndStatus(collegeId, PaymentStatus.PENDING),
                "totalCollectedAmount", totals.totalPaid(),
                "totalPendingAmount", totals.totalRemaining()));
    }

    @GetMapping("/hod")
    public ApiResponse<?> hod() {
        Long collegeId = scopeCollege(null);
        return ApiResponse.success("HOD dashboard", Map.of(
                "totalClasses", classes.countByCollegeId(collegeId),
                "totalSections", sections.countByCollegeId(collegeId),
                "totalSubjects", subjects.countByCollegeId(collegeId),
                "totalStudents", students.countByCollegeId(collegeId),
                "todayAttendanceSessions", 0));
    }

    @GetMapping("/teacher")
    public ApiResponse<?> teacher() {
        var staffProfile = staff.findByUserId(SecurityUtils.getCurrentUserId()).orElse(null);
        long subjectCount = staffProfile == null || staffProfile.getDepartment() == null
                ? 0 : subjects.countByDepartmentId(staffProfile.getDepartment().getId());
        return ApiResponse.success("Teacher dashboard", Map.of(
                "mySubjects", subjectCount,
                "mySections", 0,
                "todayClasses", 0,
                "pendingAttendanceSessions", 0,
                "submittedAttendanceSessions", 0));
    }

    @GetMapping("/student")
    public ApiResponse<?> student() {
        var student = students.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow();
        var fee = fees.findTopByStudentIdOrderByCreatedAtDesc(student.getId()).orElse(null);
        return ApiResponse.success("Student dashboard", Map.of(
                "studentName", student.getFullName(),
                "collegeName", student.getCollege().getName(),
                "departmentName", student.getDepartment().getName(),
                "admissionStatus", student.getStatus(),
                "feeStatus", fee == null ? "NOT_CREATED" : fee.getStatus(),
                "totalFee", fee == null ? BigDecimal.ZERO : fee.getTotalFee(),
                "paidAmount", fee == null ? BigDecimal.ZERO : fee.getPaidAmount(),
                "remainingAmount", fee == null ? BigDecimal.ZERO : fee.getRemainingAmount(),
                "attendancePercentage", 0));
    }

    private Map<String, Object> college(Long collegeId) {
        FeeBalanceTotals totals = fees.balanceTotalsByCollegeId(collegeId);
        return Map.ofEntries(
                Map.entry("collegeId", collegeId),
                Map.entry("totalDepartments", departments.countByCollegeId(collegeId)),
                Map.entry("totalStaff", staff.countByCollegeId(collegeId)),
                Map.entry("totalStudents", students.countByCollegeId(collegeId)),
                Map.entry("pendingAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.PRINCIPAL_REVIEW_PENDING)),
                Map.entry("approvedAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.PRINCIPAL_APPROVED)),
                Map.entry("rejectedAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.PRINCIPAL_REJECTED)),
                Map.entry("totalFeeCollected", totals.totalPaid()),
                Map.entry("totalFeePending", totals.totalRemaining()),
                Map.entry("totalClasses", classes.countByCollegeId(collegeId)),
                Map.entry("totalSections", sections.countByCollegeId(collegeId)),
                Map.entry("totalSubjects", subjects.countByCollegeId(collegeId)));
    }

    private Long scopeCollege(Long requested) {
        if (SecurityUtils.isSuperAdmin()) {
            if (requested == null) throw new IllegalArgumentException("collegeId is required for Super Admin");
            return requested;
        }
        return SecurityUtils.requireCurrentUser().getCollegeId();
    }

}
