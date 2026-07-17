package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.dto.FeeCollectionRow;
import com.jadhavr.erp.fee.dto.PendingFeeRow;
import com.jadhavr.erp.fee.dto.PendingFeeSummary;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import java.math.BigDecimal;

@Transactional(readOnly = true)
@RestController
@RequestMapping("/api/super-admin")
public class SuperAdminFeeAnalyticsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final StudentFeeAccountRepository accounts;
    private final FeePaymentRepository payments;
    private final CollegeRepository colleges;
    private final UserRepository users;
    private final StaffProfileRepository staff;
    private final StudentProfileRepository students;
    private final AdmissionFormRepository admissions;
    private final StudentSectionEnrollmentRepository enrollments;

    public SuperAdminFeeAnalyticsController(
            StudentFeeAccountRepository accounts,
            FeePaymentRepository payments,
            CollegeRepository colleges,
            UserRepository users,
            StaffProfileRepository staff,
            StudentProfileRepository students,
            AdmissionFormRepository admissions,
            StudentSectionEnrollmentRepository enrollments) {
        this.accounts = accounts;
        this.payments = payments;
        this.colleges = colleges;
        this.users = users;
        this.staff = staff;
        this.students = students;
        this.admissions = admissions;
        this.enrollments = enrollments;
    }

    @GetMapping("/fees/collection-summary")
    public ApiResponse<Map<String, Object>> collectionSummary() {
        return ApiResponse.success("Collection summary", summary());
    }

    @GetMapping("/fees/pending-summary")
    public ApiResponse<PendingFeeSummary> pendingSummary() {
        return ApiResponse.success("Pending summary", accounts.pendingFeeSummary());
    }

    @GetMapping("/fees/collections")
    public ApiResponse<PageResponse<FeeCollectionRow>> collections(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = page(page, size, "createdAt");
        return ApiResponse.success(
                "Fee collections",
                PageResponse.from(payments.findVerifiedCollections(
                        collegeId,
                        departmentId,
                        normalize(academicYear),
                        studentCategory,
                        normalizeKeyword(keyword), courseYearId, divisionId,
                        pageable)));
    }

    @GetMapping("/fees/pending")
    public ApiResponse<PageResponse<PendingFeeRow>> pending(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = page(page, size, "remainingAmount");
        return ApiResponse.success(
                "Pending fees",
                PageResponse.from(accounts.findPendingFees(
                        collegeId,
                        departmentId,
                        normalize(academicYear),
                        studentCategory,
                        normalizeKeyword(keyword), courseYearId, divisionId,
                        pageable)));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId) {
        Set<Long> scopedStudentIds = enrollments.findAll().stream()
                .filter(e -> e.getStatus() == AcademicStatus.ACTIVE)
                .filter(e -> collegeId == null || collegeId.equals(e.getSection().getCollege().getId()))
                .filter(e -> departmentId == null || departmentId.equals(e.getSection().getDepartment().getId()))
                .filter(e -> courseYearId == null || courseYearId.equals(e.getAcademicClass().getId()))
                .filter(e -> divisionId == null || divisionId.equals(e.getSection().getId()))
                .map(e -> e.getStudent().getId()).collect(Collectors.toSet());
        boolean academicScope = courseYearId != null || divisionId != null;
        Map<String, Long> admissionDistribution = new LinkedHashMap<>();
        admissions.findAll().stream()
                .filter(a -> collegeId == null || collegeId.equals(a.getCollege().getId()))
                .filter(a -> departmentId == null || departmentId.equals(a.getDepartment().getId()))
                .filter(a -> !academicScope || (a.getStudent() != null && scopedStudentIds.contains(a.getStudent().getId())))
                .forEach(a -> admissionDistribution.merge(a.getStatus().name(), 1L, Long::sum));

        List<Map<String, Object>> largestPendingFees = accounts.findPendingFees(
                        null,
                        null,
                        null,
                        null,
                        "",
                        null,
                        null,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "remainingAmount")))
                .getContent()
                .stream()
                .map(row -> Map.<String, Object>of(
                        "student", row.studentName(),
                        "college", row.collegeName(),
                        "remaining", row.remainingAmount()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        var scopedStudents = students.findAll().stream()
                .filter(s -> collegeId == null || collegeId.equals(s.getCollege().getId()))
                .filter(s -> departmentId == null || departmentId.equals(s.getDepartment().getId()))
                .filter(s -> !academicScope || scopedStudentIds.contains(s.getId())).toList();
        var scopedAccounts = accounts.findAll().stream()
                .filter(a -> collegeId == null || collegeId.equals(a.getCollege().getId()))
                .filter(a -> departmentId == null || departmentId.equals(a.getDepartment().getId()))
                .filter(a -> !academicScope || scopedStudentIds.contains(a.getStudent().getId())).toList();
        BigDecimal paid = scopedAccounts.stream().map(a -> a.getPaidAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = scopedAccounts.stream().map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> filteredSummary = new LinkedHashMap<>(summary());
        if (collegeId != null) {
            filteredSummary.put("totalColleges", colleges.existsById(collegeId) ? 1L : 0L);
            filteredSummary.put("activeColleges", colleges.findById(collegeId)
                    .filter(c -> c.getStatus() == CollegeStatus.ACTIVE).isPresent() ? 1L : 0L);
            filteredSummary.put("totalPrincipals", users.findAll().stream()
                    .filter(u -> u.getCollege() != null && collegeId.equals(u.getCollege().getId()))
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.PRINCIPAL)).count());
        }
        filteredSummary.put("totalStudents", (long) scopedStudents.size());
        filteredSummary.put("totalStaff", staff.findAll().stream()
                .filter(s -> collegeId == null || collegeId.equals(s.getCollege().getId()))
                .filter(s -> departmentId == null || s.belongsToDepartment(departmentId)).count());
        filteredSummary.put("totalFeeCollection", paid);
        filteredSummary.put("pendingFee", pending);
        result.put("summary", filteredSummary);
        result.put("collegeWiseStudents", scopedStudents.stream().collect(Collectors.groupingBy(
                s -> s.getCollege().getName(), LinkedHashMap::new, Collectors.counting())).entrySet().stream()
                .map(e -> Map.of("label", e.getKey(), "value", e.getValue())).toList());
        result.put("collegeWiseFeeCollection", scopedAccounts.stream().collect(Collectors.groupingBy(
                a -> a.getCollege().getName(), LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, a -> a.getPaidAmount(), BigDecimal::add))).entrySet().stream()
                .map(e -> Map.of("label", e.getKey(), "value", e.getValue())).toList());
        result.put("admissionStatusDistribution", admissionDistribution);
        result.put("pendingFees", largestPendingFees);
        return ApiResponse.success("Admin analytics", result);
    }

    private Map<String, Object> summary() {
        return Map.of(
                "totalColleges", colleges.count(),
                "activeColleges", colleges.countByStatus(CollegeStatus.ACTIVE),
                "totalPrincipals", users.countByRole(RoleName.PRINCIPAL),
                "totalStaff", staff.count(),
                "totalStudents", students.count(),
                "totalFeeCollection", accounts.sumPaidAmount(),
                "pendingFee", accounts.sumRemainingAmount());
    }

    private Pageable page(int page, int size, String sortField) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeKeyword(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
