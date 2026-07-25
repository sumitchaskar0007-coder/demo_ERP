package com.jadhavr.erp.reports.service;

import com.jadhavr.erp.academic.entity.StudentSectionEnrollment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.admission.entity.*;
import com.jadhavr.erp.admission.enums.*;
import com.jadhavr.erp.admission.repository.*;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.fee.enums.FeeAccountStatus;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.reports.dto.AdmissionAnalyticsDtos.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdmissionAnalyticsService {
    private static final Set<AdmissionStatus> APPROVED = Set.of(AdmissionStatus.PRINCIPAL_APPROVED);
    private static final Set<AdmissionStatus> REJECTED = Set.of(AdmissionStatus.STUDENT_SECTION_REJECTED, AdmissionStatus.PRINCIPAL_REJECTED);
    private final AdmissionFormRepository admissions;
    private final AdmissionStatusHistoryRepository histories;
    private final StudentFeeAccountRepository fees;
    private final StudentSectionEnrollmentRepository enrollments;

    public AdmissionAnalyticsService(AdmissionFormRepository admissions, AdmissionStatusHistoryRepository histories,
            StudentFeeAccountRepository fees, StudentSectionEnrollmentRepository enrollments) {
        this.admissions = admissions; this.histories = histories; this.fees = fees; this.enrollments = enrollments;
    }

    public AnalyticsResponse analytics(Long requestedCollegeId, Long departmentId, String academicYear,
            String status, LocalDate from, LocalDate to, String studentName,
            String admissionNumber, String mobile, int page, int size, String sort) {
        Long collegeId = scope(requestedCollegeId);
        if (from != null && to != null && from.isAfter(to)) throw new BadRequestException("From date cannot be after to date");
        Specification<AdmissionForm> spec = Specification.where(null);
        if (collegeId != null) spec = spec.and((r,q,cb) -> cb.equal(r.get("college").get("id"), collegeId));
        if (departmentId != null) spec = spec.and((r,q,cb) -> cb.equal(r.get("department").get("id"), departmentId));
        if (academicYear != null && !academicYear.isBlank()) spec = spec.and((r,q,cb) -> cb.equal(r.get("academicYear"), academicYear));
        if (status != null && !status.isBlank()) {
            try { AdmissionStatus parsed = AdmissionStatus.valueOf(status); spec = spec.and((r,q,cb) -> cb.equal(r.get("status"), parsed)); }
            catch (IllegalArgumentException ex) { throw new BadRequestException("Invalid admission status"); }
        }
        if (from != null) spec = spec.and((r,q,cb) -> cb.greaterThanOrEqualTo(r.get("submittedAt"), from.atStartOfDay()));
        if (to != null) spec = spec.and((r,q,cb) -> cb.lessThan(r.get("submittedAt"), to.plusDays(1).atStartOfDay()));
        if (has(studentName)) { String value = "%" + studentName.trim().toLowerCase() + "%"; spec = spec.and((r,q,cb) -> cb.like(cb.lower(r.get("fullName")), value)); }
        if (has(admissionNumber)) { String value = "%" + admissionNumber.trim().toLowerCase() + "%"; spec = spec.and((r,q,cb) -> cb.or(cb.like(cb.lower(r.get("admissionReferenceNumber")), value), cb.like(cb.lower(r.get("student").get("admissionNumber")), value))); }
        if (has(mobile)) { String value = "%" + mobile.trim() + "%"; spec = spec.and((r,q,cb) -> cb.like(r.get("phone"), value)); }

        Sort ordering = "oldest".equals(sort) ? Sort.by("submittedAt").ascending() : Sort.by("submittedAt").descending();
        int safeSize = Math.min(100, Math.max(10, size)); int safePage = Math.max(0, page);
        Page<AdmissionForm> result = admissions.findAll(spec, PageRequest.of(safePage, safeSize, ordering));
        List<AdmissionForm> all = admissions.findAll(spec, Sort.by("submittedAt").ascending());
        List<Long> allIds = all.stream().map(AdmissionForm::getId).toList();
        Map<Long, StudentFeeAccount> allFees = allIds.isEmpty() ? Map.of() : fees.findByAdmissionFormIdIn(allIds).stream()
                .collect(Collectors.toMap(a -> a.getAdmissionForm().getId(), a -> a, (a,b) -> a));
        List<Long> allStudentIds = all.stream().map(a -> a.getStudent().getId()).toList();
        Map<Long, StudentSectionEnrollment> allEnrollments = allStudentIds.isEmpty() ? Map.of() : enrollments
                .findByStudentIdInAndStatus(allStudentIds, AcademicStatus.ACTIVE).stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e, (a,b) -> a));
        List<Long> pageIds = result.getContent().stream().map(AdmissionForm::getId).toList();
        Map<Long, List<AdmissionStatusHistory>> pageHistory = pageIds.isEmpty() ? Map.of() : histories
                .findByAdmissionFormIdInOrderByCreatedAtAsc(pageIds).stream()
                .collect(Collectors.groupingBy(h -> h.getAdmissionForm().getId(), LinkedHashMap::new, Collectors.toList()));
        List<AdmissionRow> rows = result.getContent().stream().map(a -> row(a, allFees.get(a.getId()),
                allEnrollments.get(a.getStudent().getId()), pageHistory.getOrDefault(a.getId(), List.of()))).toList();

        LocalDate now = LocalDate.now(); long approved = all.stream().filter(a -> APPROVED.contains(a.getStatus())).count();
        long rejected = all.stream().filter(a -> REJECTED.contains(a.getStatus())).count();
        long cancelled = all.stream().filter(a -> a.getStatus() == AdmissionStatus.CANCELLED).count();
        long pending = all.size() - approved - rejected - cancelled;
        long todayCount = all.stream().filter(a -> a.getSubmittedAt().toLocalDate().equals(now)).count();
        long docsPending = all.stream().filter(a -> a.getDetailsCompletedAt() == null).count();
        long feePending = all.stream().filter(a -> feePending(allFees.get(a.getId()))).count();
        double avgDays = all.stream().filter(a -> terminal(a.getStatus())).mapToLong(this::processingDays).average().orElse(0);
        Summary summary = new Summary(all.size(), approved, pending, rejected, todayCount,
                all.isEmpty() ? 0 : round(approved * 100.0 / all.size()), docsPending, feePending, round(avgDays));
        List<FunnelStage> funnel = List.of(
                new FunnelStage("received", "Applications Received", all.size()),
                new FunnelStage("documents", "Documents Uploaded", all.stream().filter(a -> a.getDetailsCompletedAt() != null).count()),
                new FunnelStage("verification", "Verification Completed", all.stream().filter(a -> a.getStudentSectionVerifiedAt() != null).count()),
                new FunnelStage("approved", "Principal Approved", approved),
                new FunnelStage("fee", "Fee Paid", all.stream().filter(a -> feePaid(allFees.get(a.getId()))).count()),
                new FunnelStage("completed", "Admission Completed", all.stream().filter(a -> APPROVED.contains(a.getStatus()) && feePaid(allFees.get(a.getId()))).count()));
        List<TrendPoint> trend = all.stream().collect(Collectors.groupingBy(a -> a.getSubmittedAt().toLocalDate(), TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> new TrendPoint(e.getKey(), e.getValue().size(),
                        e.getValue().stream().filter(a -> APPROVED.contains(a.getStatus())).count(),
                        e.getValue().stream().filter(a -> REJECTED.contains(a.getStatus())).count())).toList();
        List<GroupAnalytics> departmentGroups = groups(all, a -> String.valueOf(a.getDepartment().getId()),
                a -> a.getDepartment().getName(), a -> "", a -> "—");
        List<AdmissionForm> allocated = all.stream().filter(a -> allEnrollments.containsKey(a.getStudent().getId())).toList();
        List<GroupAnalytics> yearGroups = groups(allocated,
                a -> a.getDepartment().getId() + "|" + allEnrollments.get(a.getStudent().getId()).getAcademicClass().getName(),
                a -> allEnrollments.get(a.getStudent().getId()).getAcademicClass().getName(),
                a -> String.valueOf(a.getDepartment().getId()), a -> "—");
        List<GroupAnalytics> divisionGroups = groups(allocated,
                a -> String.valueOf(allEnrollments.get(a.getStudent().getId()).getSection().getId()),
                a -> allEnrollments.get(a.getStudent().getId()).getSection().getName(),
                a -> a.getDepartment().getId() + "|" + allEnrollments.get(a.getStudent().getId()).getAcademicClass().getName(),
                a -> allEnrollments.get(a.getStudent().getId()).getSection().getClassTeacher() == null ? "Unassigned" : allEnrollments.get(a.getStudent().getId()).getSection().getClassTeacher().getFullName());
        return new AnalyticsResponse(summary, funnel, trend, departmentGroups, yearGroups, divisionGroups,
                rows, result.getTotalElements(),
                result.getTotalPages(), result.getNumber(), result.getSize());
    }

    private List<GroupAnalytics> groups(List<AdmissionForm> forms,
            java.util.function.Function<AdmissionForm,String> key,
            java.util.function.Function<AdmissionForm,String> label,
            java.util.function.Function<AdmissionForm,String> parent,
            java.util.function.Function<AdmissionForm,String> teacher) {
        return forms.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> { List<AdmissionForm> g=e.getValue(); AdmissionForm first=g.get(0);
                    long approved=g.stream().filter(a->APPROVED.contains(a.getStatus())).count();
                    long rejected=g.stream().filter(a->REJECTED.contains(a.getStatus())).count();
                    long pending=g.size()-approved-rejected-g.stream().filter(a->a.getStatus()==AdmissionStatus.CANCELLED).count();
                    return new GroupAnalytics(e.getKey(),label.apply(first),parent.apply(first),g.size(),approved,pending,rejected,
                            round(approved*100.0/g.size()),teacher.apply(first));
                }).sorted(Comparator.comparing(GroupAnalytics::label,String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private AdmissionRow row(AdmissionForm a, StudentFeeAccount fee, StudentSectionEnrollment enrollment,
            List<AdmissionStatusHistory> history) {
        String address = String.join(", ", nonBlank(a.getAddressLine1(), a.getAddressLine2(), a.getCity(), a.getState(), a.getPincode()));
        String remarks = has(a.getRejectionReason()) ? a.getRejectionReason() : a.getStudentSectionRemarks();
        List<TimelineItem> timeline = new ArrayList<>();
        timeline.add(new TimelineItem("Application Submitted", "COMPLETED", a.getSubmittedAt(), a.getFullName(), null));
        if (a.getDetailsCompletedAt() != null) timeline.add(new TimelineItem("Documents Uploaded", "COMPLETED", a.getDetailsCompletedAt(), a.getFullName(), null));
        history.forEach(h -> timeline.add(new TimelineItem(stage(h.getAction()), h.getNewStatus().name(), h.getCreatedAt(),
                h.getChangedBy() == null ? "System" : h.getChangedBy().getFullName(), h.getRemarks())));
        return new AdmissionRow(a.getId(), a.getAdmissionReferenceNumber(), a.getStudent().getAdmissionNumber(),
                a.getFullName(), a.getGender(), a.getDateOfBirth(), a.getEmail(), a.getPhone(), a.getParentName(),
                a.getParentPhone(), address, a.getCollege().getId(), a.getCollege().getName(), a.getDepartment().getId(),
                a.getDepartment().getName(), a.getDepartment().getCode(), a.getAcademicYear(),
                enrollment == null ? "Unallocated" : enrollment.getAcademicClass().getName(),
                enrollment == null ? null : enrollment.getSection().getId(), enrollment == null ? "Unallocated" : enrollment.getSection().getName(),
                enrollment == null || enrollment.getSection().getClassTeacher() == null ? "Unassigned" : enrollment.getSection().getClassTeacher().getFullName(),
                a.getStatus().name(), a.getSubmittedAt(), a.getPrincipalApprovedAt(), a.getDetailsCompletedAt() != null,
                feePaid(fee), feePending(fee), remarks, processingDays(a), timeline);
    }

    private int processingDays(AdmissionForm a) { LocalDateTime end = a.getPrincipalApprovedAt() != null ? a.getPrincipalApprovedAt() : a.getUpdatedAt(); return (int) Math.max(0, ChronoUnit.DAYS.between(a.getSubmittedAt(), end)); }
    private boolean terminal(AdmissionStatus status) { return APPROVED.contains(status) || REJECTED.contains(status) || status == AdmissionStatus.CANCELLED; }
    private boolean feePaid(StudentFeeAccount a) { return a != null && (a.getStatus() == FeeAccountStatus.PAID || a.getPaidAmount().compareTo(a.getMinimumAmountForAdmission()) >= 0); }
    private boolean feePending(StudentFeeAccount a) { return a == null || !feePaid(a); }
    private String stage(AdmissionAction action) { return switch (action) { case STUDENT_SECTION_REVIEW_STARTED -> "Document Verification"; case STUDENT_SECTION_APPROVED -> "Verification Completed"; case PRINCIPAL_REVIEW_PENDING -> "Principal Review"; case PRINCIPAL_APPROVED -> "Principal Approved"; case PAYMENT_SUBMITTED, PAYMENT_VERIFIED -> "Fee Payment"; default -> action.name().replace('_', ' '); }; }
    private Long scope(Long requested) { if (SecurityUtils.isSuperAdmin()) return requested; Long own = SecurityUtils.requireCurrentUser().getCollegeId(); if (requested != null && !requested.equals(own)) throw new org.springframework.security.access.AccessDeniedException("Report is outside your college"); return own; }
    private boolean has(String s) { return s != null && !s.isBlank(); }
    private List<String> nonBlank(String... values) { return Arrays.stream(values).filter(this::has).toList(); }
    private double round(double n) { return Math.round(n * 100.0) / 100.0; }
}
