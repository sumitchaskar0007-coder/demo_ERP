package com.collegeerp.erp.analytics.controller;

import com.collegeerp.erp.academic.repository.AcademicClassRepository;
import com.collegeerp.erp.academic.repository.AttendanceSessionRepository;
import com.collegeerp.erp.academic.repository.SectionRepository;
import com.collegeerp.erp.academic.repository.SubjectRepository;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.api.ApiResponse;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.fee.dto.FeeBalanceTotals;
import com.collegeerp.erp.fee.enums.PaymentStatus;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.user.entity.RoleName;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ApiResponse<?> principal(@RequestParam(required = false) Long collegeId) {
        return ApiResponse.success("Principal dashboard", college(scopeCollege(collegeId)));
    }

    @GetMapping("/student-section")
    @PreAuthorize("hasRole('STUDENT_SECTION')")
    public ApiResponse<?> studentSection() {
        Long collegeId = scopeCollege(null);
        return ApiResponse.success("Student Section dashboard", Map.of(
                "submittedAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.SUBMITTED),
                "reviewPendingAdmissions", admissions.countByCollegeIdAndStatus(collegeId, AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING),
                "approvedByStudentSection", admissions.countByCollegeIdAndStatusIn(collegeId, Set.of(
                        AdmissionStatus.STUDENT_SECTION_APPROVED,
                        AdmissionStatus.PRINCIPAL_REVIEW_PENDING,
                        AdmissionStatus.PRINCIPAL_APPROVED)),
                "rejectedByStudentSection", admissions.countByCollegeIdAndStatusIn(collegeId, Set.of(
                        AdmissionStatus.STUDENT_SECTION_REJECTED,
                        AdmissionStatus.PRINCIPAL_REJECTED)),
                "printedForms", admissions.countByCollegeIdAndPrintCountGreaterThan(collegeId, 0)));
    }

    @GetMapping("/fee-section")
    @PreAuthorize("hasRole('FEE_SECTION')")
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
    @PreAuthorize("hasRole('HOD')")
    public ApiResponse<?> hod() {
        var profile = staff.findByUserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("HOD profile is required"));
        if (profile.getDepartment() == null) {
            throw new org.springframework.security.access.AccessDeniedException("HOD department is required");
        }
        Long departmentId = profile.getDepartment().getId();
        return ApiResponse.success("HOD dashboard", Map.of(
                "totalClasses", classes.countByDepartmentId(departmentId),
                "totalSections", sections.countByDepartmentId(departmentId),
                "totalSubjects", subjects.countByDepartmentId(departmentId),
                "totalStudents", students.countByDepartmentId(departmentId),
                "todayAttendanceSessions", 0));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('CLASS_TEACHER','SUBJECT_TEACHER')")
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
    @PreAuthorize("hasRole('STUDENT')")
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
                Map.entry("totalTeachingStaff", staff.countTeachingStaffByCollegeId(collegeId,
                        Set.of(RoleName.HOD, RoleName.CLASS_TEACHER, RoleName.SUBJECT_TEACHER))),
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
        Long ownCollegeId = SecurityUtils.requireCurrentUser().getCollegeId();
        if (requested != null && !requested.equals(ownCollegeId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Dashboard is outside your college");
        }
        return ownCollegeId;
    }

}
