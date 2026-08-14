package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.analytics.repository.AdminAnalyticsReadRepository;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.dto.FeeCollectionRow;
import com.jadhavr.erp.fee.dto.FeeReportPageResponse;
import com.jadhavr.erp.fee.dto.PendingFeeRow;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/principal")
@Transactional(readOnly = true)
public class PrincipalFeeAnalyticsController {
    private final FeePaymentRepository payments;
    private final StudentFeeAccountRepository accounts;
    private final AdminAnalyticsReadRepository analytics;

    public PrincipalFeeAnalyticsController(FeePaymentRepository payments,
            StudentFeeAccountRepository accounts, AdminAnalyticsReadRepository analytics) {
        this.payments = payments;
        this.accounts = accounts;
        this.analytics = analytics;
    }

    @GetMapping("/fees/collections")
    public ApiResponse<FeeReportPageResponse<FeeCollectionRow>> collections(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long scopedCollegeId = collegeId();
        String normalizedYear = clean(academicYear);
        String normalizedKeyword = keyword(keyword);
        return ApiResponse.success("Fee collections", FeeReportPageResponse.from(
                payments.findVerifiedCollections(scopedCollegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId,
                        page(page, size, "createdAt")),
                payments.sumVerifiedCollections(scopedCollegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId)));
    }

    @GetMapping("/fees/pending")
    public ApiResponse<FeeReportPageResponse<PendingFeeRow>> pending(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) StudentCategory studentCategory,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long scopedCollegeId = collegeId();
        String normalizedYear = clean(academicYear);
        String normalizedKeyword = keyword(keyword);
        return ApiResponse.success("Pending fees", FeeReportPageResponse.from(
                accounts.findPendingFees(scopedCollegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId,
                        page(page, size, "remainingAmount")),
                accounts.sumPendingFees(scopedCollegeId, departmentId, normalizedYear,
                        studentCategory, normalizedKeyword, courseYearId, divisionId)));
    }

    @GetMapping("/analytics")
    public ApiResponse<Map<String, Object>> analytics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long courseYearId,
            @RequestParam(required = false) Long divisionId) {
        return ApiResponse.success("Principal analytics",
                analytics.read(collegeId(), departmentId, courseYearId, divisionId));
    }

    private Long collegeId() {
        return SecurityUtils.requireCurrentUser().getCollegeId();
    }

    private PageRequest page(int page, int size, String field) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Invalid pagination");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, field));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String keyword(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
