package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.fee.dto.ChangeStudentCategoryRequest;
import com.collegeerp.erp.fee.dto.FeeCategoryAssessmentOptionResponse;
import com.collegeerp.erp.fee.dto.RemoveScholarshipRequest;
import com.collegeerp.erp.fee.dto.ScholarshipResponse;
import com.collegeerp.erp.fee.dto.StudentFeeAccountResponse;
import com.collegeerp.erp.fee.entity.FeeStructure;
import com.collegeerp.erp.fee.entity.FeeTransaction;
import com.collegeerp.erp.fee.entity.StudentFeeAccount;
import com.collegeerp.erp.fee.enums.FeeAccountStatus;
import com.collegeerp.erp.fee.enums.FeeStructureStatus;
import com.collegeerp.erp.fee.enums.FeeTransactionType;
import com.collegeerp.erp.fee.repository.FeeStructureRepository;
import com.collegeerp.erp.fee.repository.FeeTransactionRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.notice.entity.NoticePriority;
import com.collegeerp.erp.notice.service.NoticeService;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
public class PrincipalFeeAdjustmentService {
    private final StudentFeeAccountRepository accounts;
    private final FeeStructureRepository structures;
    private final FeeTransactionRepository transactions;
    private final AdmissionFormRepository admissions;
    private final StudentSectionEnrollmentRepository enrollments;
    private final UserRepository users;
    private final NoticeService notices;

    public PrincipalFeeAdjustmentService(
            StudentFeeAccountRepository accounts,
            FeeStructureRepository structures,
            FeeTransactionRepository transactions,
            AdmissionFormRepository admissions,
            StudentSectionEnrollmentRepository enrollments,
            UserRepository users,
            NoticeService notices) {
        this.accounts = accounts;
        this.structures = structures;
        this.transactions = transactions;
        this.admissions = admissions;
        this.enrollments = enrollments;
        this.users = users;
        this.notices = notices;
    }

    @Transactional(readOnly = true)
    public List<FeeCategoryAssessmentOptionResponse> categoryOptions(Long studentId) {
        StudentFeeAccount account = scopedAccount(studentId, false);
        AdmissionForm admission = account.getAdmissionForm();
        requireRegularAccount(account);
        String courseYear = courseYear(account, admission);
        String gender = FeeCategoryRules.normalizeGender(admission.getGender());
        List<FeeStructure> configured = structures.findAssessmentOptions(
                account.getCollege().getId(), account.getDepartment().getId(),
                FeeCategoryRules.academicYearVariants(account.getAcademicYear()), gender,
                courseYear);
        var unique = new LinkedHashMap<String, FeeStructure>();
        for (FeeStructure structure : configured) {
            String custom = structure.getCustomCategoryName();
            String key = structure.getStudentCategory().name() + ":"
                    + (custom == null ? "" : custom.trim().toUpperCase(Locale.ROOT));
            if (unique.putIfAbsent(key, structure) != null) {
                throw new BadRequestException(
                        "Duplicate active fee structures exist for " + label(structure)
                                + " and " + gender + ". Deactivate the duplicate configuration.");
            }
        }
        return unique.values().stream().map(this::option).toList();
    }

    @Transactional
    public ScholarshipResponse removeScholarship(Long studentId, RemoveScholarshipRequest request) {
        StudentFeeAccount account = scopedAccount(studentId, true);
        requireRegularAccount(account);
        if (account.isScholarshipRemoved()) return scholarship(account, null, null);
        if (account.getDiscountAmount().signum() <= 0) {
            throw new BadRequestException("This student does not currently have a scholarship");
        }

        User principal = currentPrincipal();
        BigDecimal removed = account.getDiscountAmount();
        BigDecimal oldRemaining = account.getRemainingAmount();
        account.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        account.setScholarshipRemoved(true);
        account.setScholarshipRemovedAt(LocalDateTime.now());
        account.setScholarshipRemovalReason(request.reason().trim());
        if (account.getFeeStructure() != null) {
            account.setMinimumAmountForAdmission(
                    account.getFeeStructure().getMinimumAmountForAdmission().min(account.getTotalFee()));
        }
        recalculateBalance(account);
        accounts.save(account);
        audit(account, FeeTransactionType.SCHOLARSHIP_REMOVED, removed, oldRemaining,
                "All scholarship removed: " + request.reason().trim(), principal);

        notices.createUserWorkflowNotice(
                "Scholarship removed",
                "Your scholarship of ₹" + removed.toPlainString()
                        + " was removed by the Principal. Your remaining fee balance is ₹"
                        + account.getRemainingAmount().toPlainString() + ".",
                NoticePriority.HIGH, RoleName.STUDENT, account.getCollege(),
                account.getStudentUser(), "/student/fees");
        return scholarship(account, principal, account.getScholarshipRemovedAt());
    }

    @Transactional
    public StudentFeeAccountResponse changeCategory(
            Long studentId, ChangeStudentCategoryRequest request) {
        StudentFeeAccount account = scopedAccount(studentId, true);
        requireRegularAccount(account);
        AdmissionForm admission = account.getAdmissionForm();
        String courseYear = courseYear(account, admission);
        String custom = FeeCategoryRules.normalizeCustomCategory(
                request.studentCategory(), request.customCategoryName());
        String gender = FeeCategoryRules.normalizeGender(admission.getGender());
        List<FeeStructure> matches = structures.findConfiguredAssessments(
                account.getCollege().getId(), account.getDepartment().getId(),
                FeeCategoryRules.academicYearVariants(account.getAcademicYear()),
                request.studentCategory(), custom, gender,
                courseYear, FeeStructureStatus.ACTIVE);
        FeeStructure target = matches.stream().findFirst().orElseThrow(() ->
                new BadRequestException("No active "
                        + (custom == null ? request.studentCategory().name() : custom)
                        + " fee structure exists for this course year, " + gender
                        + " and academic year"));

        User principal = currentPrincipal();
        String previousLabel = account.getCustomCategoryName() == null
                ? account.getStudentCategory().name() : account.getCustomCategoryName();
        BigDecimal oldRemaining = account.getRemainingAmount();
        BigDecimal oldPayable = account.getTotalFee().subtract(account.getDiscountAmount());

        account.setStudentCategory(request.studentCategory());
        account.setCustomCategoryName(custom);
        account.setFeeStructure(target);
        account.setTotalFee(target.getTotalFee());
        account.setDiscountAmount(account.isScholarshipRemoved()
                ? BigDecimal.ZERO.setScale(2) : target.getScholarshipAmount());
        BigDecimal newPayable = account.getTotalFee().subtract(account.getDiscountAmount());
        account.setMinimumAmountForAdmission(target.getMinimumAmountForAdmission().min(newPayable));
        recalculateBalance(account);

        admission.setCaste(request.caste().trim());
        admission.setStudentCategory(request.studentCategory());
        admission.setCustomCategoryName(custom);
        account.getStudent().setStudentCategory(request.studentCategory());
        account.getStudent().setCustomCategoryName(custom);
        admissions.save(admission);
        accounts.save(account);

        String newLabel = custom == null ? request.studentCategory().name() : custom;
        audit(account, FeeTransactionType.FEE_CATEGORY_CHANGED,
                newPayable.subtract(oldPayable).abs(), oldRemaining,
                "Category changed from " + previousLabel + " to " + newLabel
                        + "; caste set to " + request.caste().trim()
                        + "; reason: " + request.reason().trim(), principal);
        notices.createUserWorkflowNotice(
                "Fee category updated",
                "Your verified fee category was updated to " + newLabel
                        + ". Your assessed fee is ₹" + newPayable.toPlainString()
                        + " and remaining balance is ₹" + account.getRemainingAmount().toPlainString()
                        + (account.getCreditAmount().signum() > 0
                            ? ". Credit/refund due: ₹" + account.getCreditAmount().toPlainString() : "."),
                NoticePriority.HIGH, RoleName.STUDENT, account.getCollege(),
                account.getStudentUser(), "/student/fees");
        return response(account);
    }

    private StudentFeeAccount scopedAccount(Long studentId, boolean lock) {
        if (!SecurityUtils.isPrincipal()) {
            throw new AccessDeniedException("Only Principal can adjust student fees");
        }
        StudentFeeAccount latest = accounts
                .findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regular fee account is not generated for this student"));
        StudentFeeAccount account = lock
                ? accounts.findByIdForUpdate(latest.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fee account not found"))
                : latest;
        Long ownCollege = SecurityUtils.requireCurrentUser().getCollegeId();
        if (ownCollege == null || !ownCollege.equals(account.getCollege().getId())) {
            throw new AccessDeniedException("Student is outside your college");
        }
        return account;
    }

    private void requireRegularAccount(StudentFeeAccount account) {
        if (account.getFeeStructure() == null) {
            throw new BadRequestException(
                    "Regular course fee must be allocated before adjusting scholarship or category");
        }
    }

    private String courseYear(StudentFeeAccount account, AdmissionForm admission) {
        if (admission.getCourseYear() != null) return admission.getCourseYear().getName();
        return enrollments.findByStudentIdAndAcademicYearAndStatus(
                        account.getStudent().getId(), account.getAcademicYear(), AcademicStatus.ACTIVE)
                .or(() -> enrollments.findFirstByStudentAndStatus(
                        account.getStudent(), AcademicStatus.ACTIVE))
                .map(enrollment -> enrollment.getAcademicClass().getName())
                .orElseThrow(() -> new BadRequestException(
                        "Course year is required before changing the category"));
    }

    private void recalculateBalance(StudentFeeAccount account) {
        BigDecimal payable = account.getTotalFee().subtract(account.getDiscountAmount());
        account.setRemainingAmount(payable.subtract(account.getPaidAmount()).max(BigDecimal.ZERO));
        account.setCreditAmount(account.getPaidAmount().subtract(payable).max(BigDecimal.ZERO));
        account.setStatus(account.getRemainingAmount().signum() == 0
                ? FeeAccountStatus.PAID
                : account.getPaidAmount().signum() > 0
                    ? FeeAccountStatus.PARTIALLY_PAID : FeeAccountStatus.PENDING);
    }

    private void audit(StudentFeeAccount account, FeeTransactionType type, BigDecimal amount,
            BigDecimal oldRemaining, String remarks, User principal) {
        FeeTransaction transaction = new FeeTransaction();
        transaction.setStudentFeeAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setPreviousPaidAmount(account.getPaidAmount());
        transaction.setNewPaidAmount(account.getPaidAmount());
        transaction.setPreviousRemainingAmount(oldRemaining);
        transaction.setNewRemainingAmount(account.getRemainingAmount());
        transaction.setRemarks(remarks);
        transaction.setPerformedBy(principal);
        transactions.save(transaction);
    }

    private User currentPrincipal() {
        return users.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Principal not found"));
    }

    private FeeCategoryAssessmentOptionResponse option(FeeStructure structure) {
        return new FeeCategoryAssessmentOptionResponse(
                structure.getId(), structure.getStudentCategory(), structure.getCustomCategoryName(),
                label(structure), structure.getGender(), structure.getTotalFee(),
                structure.getScholarshipAmount(),
                structure.getTotalFee().subtract(structure.getScholarshipAmount()));
    }

    private String label(FeeStructure structure) {
        return structure.getCustomCategoryName() == null
                ? structure.getStudentCategory().name() : structure.getCustomCategoryName();
    }

    private ScholarshipResponse scholarship(
            StudentFeeAccount account, User principal, LocalDateTime changedAt) {
        return new ScholarshipResponse(
                account.getId(), account.getStudent().getId(), account.getStudent().getFullName(),
                account.getStudent().getAdmissionNumber(), account.getTotalFee(),
                account.getPaidAmount(), account.getDiscountAmount(), account.getRemainingAmount(),
                account.getCreditAmount(), account.isScholarshipRemoved(),
                account.getScholarshipRemovedAt(), account.getScholarshipRemovalReason(),
                account.getStatus(), principal == null ? null : principal.getFullName(), changedAt);
    }

    private StudentFeeAccountResponse response(StudentFeeAccount account) {
        return new StudentFeeAccountResponse(
                account.getId(), account.getStudent().getId(), account.getStudentUser().getId(),
                account.getAdmissionForm().getId(), account.getAdmissionForm().getAdmissionReferenceNumber(),
                account.getStudent().getAdmissionNumber(), account.getCollege().getId(),
                account.getCollege().getName(), account.getCollege().getCode(),
                account.getDepartment().getId(), account.getDepartment().getName(),
                account.getDepartment().getCode(), account.getAcademicYear(),
                account.getStudentCategory(), account.getCustomCategoryName(), account.getTotalFee(),
                account.getPaidAmount(), account.getRemainingAmount(), account.getCreditAmount(),
                account.getDiscountAmount(), account.getDiscountAmount(),
                account.isScholarshipRemoved(), account.getScholarshipRemovedAt(),
                account.getScholarshipRemovalReason(), account.getMinimumAmountForAdmission(),
                account.getStatus(), false,
                account.getCollege().getQrCodeUrl() == null ? null : "/api/student/fees/payment-qr",
                account.getCollege().getPaymentQrAccountName(),
                "Scan the college QR code and pay manually. Then submit payment proof.",
                account.getCreatedAt(), account.getUpdatedAt());
    }
}
