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

    public SuperAdminFeeAnalyticsController(
            StudentFeeAccountRepository accounts,
            FeePaymentRepository payments,
            CollegeRepository colleges,
            UserRepository users,
            StaffProfileRepository staff,
            StudentProfileRepository students,
            AdmissionFormRepository admissions) {
        this.accounts = accounts;
        this.payments = payments;
        this.colleges = colleges;
        this.users = users;
        this.staff = staff;
        this.students = students;
        this.admissions = admissions;
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
                        pageable)));
    }

    @GetMapping("/fees/pending")
    public ApiResponse<PageResponse<PendingFeeRow>> pending(
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
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
                        pageable)));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics() {
        Map<String, Long> admissionDistribution = new LinkedHashMap<>();
        admissions.countAdmissionsByStatus().forEach(row ->
                admissionDistribution.put(row.status().name(), row.value()));

        List<Map<String, Object>> largestPendingFees = accounts.findPendingFees(
                        null,
                        null,
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
        result.put("summary", summary());
        result.put("collegeWiseStudents", students.countStudentsByCollege());
        result.put("collegeWiseFeeCollection", accounts.sumPaidByCollege());
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
}
