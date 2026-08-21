package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.fee.dto.ApproveScholarshipRequest;
import com.collegeerp.erp.fee.dto.ScholarshipResponse;
import com.collegeerp.erp.fee.entity.FeeTransaction;
import com.collegeerp.erp.fee.entity.StudentFeeAccount;
import com.collegeerp.erp.fee.enums.FeeAccountStatus;
import com.collegeerp.erp.fee.enums.FeeTransactionType;
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

@Service
public class ScholarshipService {
    private final StudentFeeAccountRepository accounts;
    private final FeeTransactionRepository transactions;
    private final UserRepository users;
    private final NoticeService notices;

    public ScholarshipService(
            StudentFeeAccountRepository accounts,
            FeeTransactionRepository transactions,
            UserRepository users,
            NoticeService notices) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.users = users;
        this.notices = notices;
    }

    @Transactional
    public ScholarshipResponse approve(Long studentId, ApproveScholarshipRequest request) {
        if (!SecurityUtils.isPrincipal()) {
            throw new AccessDeniedException("Only Principal can approve scholarships");
        }
        StudentFeeAccount found = accounts
                .findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regular fee account is not generated for this student"));
        StudentFeeAccount account = accounts.findByIdForUpdate(found.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee account not found"));
        Long ownCollege = SecurityUtils.requireCurrentUser().getCollegeId();
        if (ownCollege == null || !ownCollege.equals(account.getCollege().getId())) {
            throw new AccessDeniedException("Student is outside your college");
        }
        BigDecimal amount = request.amount().setScale(2);
        if (account.getPaidAmount().compareTo(account.getMinimumAmountForAdmission()) < 0) {
            throw new BadRequestException(
                    "Student must first pay the minimum admission fee before scholarship approval");
        }
        if (amount.compareTo(account.getRemainingAmount()) > 0) {
            throw new BadRequestException("Scholarship amount cannot exceed the remaining fee");
        }

        User principal = users.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Principal not found"));
        BigDecimal previousRemaining = account.getRemainingAmount();
        BigDecimal newRemaining = previousRemaining.subtract(amount);
        account.setDiscountAmount(account.getDiscountAmount().add(amount));
        account.setScholarshipRemoved(false);
        account.setScholarshipRemovedAt(null);
        account.setScholarshipRemovalReason(null);
        account.setCreditAmount(BigDecimal.ZERO.setScale(2));
        account.setRemainingAmount(newRemaining);
        account.setStatus(newRemaining.signum() == 0
                ? FeeAccountStatus.PAID
                : account.getPaidAmount().signum() > 0
                        ? FeeAccountStatus.PARTIALLY_PAID
                        : FeeAccountStatus.PENDING);
        accounts.save(account);

        FeeTransaction transaction = new FeeTransaction();
        transaction.setStudentFeeAccount(account);
        transaction.setTransactionType(FeeTransactionType.SCHOLARSHIP_APPROVED);
        transaction.setAmount(amount);
        transaction.setPreviousPaidAmount(account.getPaidAmount());
        transaction.setNewPaidAmount(account.getPaidAmount());
        transaction.setPreviousRemainingAmount(previousRemaining);
        transaction.setNewRemainingAmount(newRemaining);
        transaction.setRemarks(clean(request.remarks()));
        transaction.setPerformedBy(principal);
        transactions.save(transaction);

        notices.createUserWorkflowNotice(
                "Scholarship approved",
                "Your scholarship of ₹" + amount.toPlainString()
                        + " has been approved. Your remaining fee balance is ₹"
                        + newRemaining.toPlainString() + ".",
                NoticePriority.HIGH,
                RoleName.STUDENT,
                account.getCollege(),
                account.getStudentUser(),
                "/student/fees");

        return response(account, principal, transaction.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ScholarshipResponse details(Long studentId) {
        StudentFeeAccount account = accounts
                .findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regular fee account is not generated for this student"));
        Long ownCollege = SecurityUtils.requireCurrentUser().getCollegeId();
        if (!SecurityUtils.isSuperAdmin()
                && (ownCollege == null || !ownCollege.equals(account.getCollege().getId()))) {
            throw new AccessDeniedException("Student is outside your college");
        }
        return response(account, null, null);
    }

    private ScholarshipResponse response(
            StudentFeeAccount account, User approvedBy, LocalDateTime approvedAt) {
        return new ScholarshipResponse(
                account.getId(), account.getStudent().getId(), account.getStudent().getFullName(),
                account.getStudent().getAdmissionNumber(), account.getTotalFee(),
                account.getPaidAmount(), account.getDiscountAmount(), account.getRemainingAmount(),
                account.getCreditAmount(), account.isScholarshipRemoved(),
                account.getScholarshipRemovedAt(), account.getScholarshipRemovalReason(),
                account.getStatus(), approvedBy == null ? null : approvedBy.getFullName(), approvedAt);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
