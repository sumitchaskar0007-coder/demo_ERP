package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.analytics.repository.AdminAnalyticsReadRepository;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.dto.FeeCollectionRow;
import com.jadhavr.erp.fee.dto.FeeReportPageResponse;
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
import org.springframework.cache.annotation.Cacheable;
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
    private final AdminAnalyticsReadRepository analyticsRead;

    public SuperAdminFeeAnalyticsController(
            StudentFeeAccountRepository accounts,
            FeePaymentRepository payments,
            CollegeRepository colleges,
            UserRepository users,
            StaffProfileRepository staff,
            StudentProfileRepository students,
            AdmissionFormRepository admissions,
            StudentSectionEnrollmentRepository enrollments,
            AdminAnalyticsReadRepository analyticsRead) {
        this.accounts = accounts;
        this.payments = payments;
        this.colleges = colleges;
        this.users = users;
        this.staff = staff;
        this.students = students;
        this.admissions = admissions;
        this.enrollments = enrollments;
        this.analyticsRead = analyticsRead;
    }

    @GetMapping("/fees/collection-summary")
    @Cacheable(cacheNames = "feeSummary", key = "'collection'", sync = true)
    public ApiResponse<Map<String, Object>> collectionSummary() {
        return ApiResponse.success("Collection summary", summary());
    }

    @GetMapping("/fees/pending-summary")
    @Cacheable(cacheNames = "feeSummary", key = "'pending'", sync = true)
    public ApiResponse<PendingFeeSummary> pendingSummary() {
        return ApiResponse.success("Pending summary", accounts.pendingFeeSummary());
    }

    @GetMapping("/fees/collections")
    public ApiResponse<FeeReportPageResponse<FeeCollectionRow>> collections(
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
        String normalizedYear = normalize(academicYear);
        String normalizedKeyword = normalizeKeyword(keyword);
        return ApiResponse.success("Fee collections", FeeReportPageResponse.from(
                payments.findVerifiedCollections(collegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId, pageable),
                payments.sumVerifiedCollections(collegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId)));
    }

    @GetMapping("/fees/pending")
    public ApiResponse<FeeReportPageResponse<PendingFeeRow>> pending(
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
        String normalizedYear = normalize(academicYear);
        String normalizedKeyword = normalizeKeyword(keyword);
        return ApiResponse.success("Pending fees", FeeReportPageResponse.from(
                accounts.findPendingFees(collegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId, pageable),
                accounts.sumPendingFees(collegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId)));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId) {
        return ApiResponse.success("Admin analytics",
                analyticsRead.read(collegeId, departmentId, courseYearId, divisionId));
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
