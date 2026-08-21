package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.service.CollegeImageStorageService;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.fee.dto.FeeReceiptResponse;
import com.collegeerp.erp.fee.entity.FeePayment;
import com.collegeerp.erp.fee.entity.StudentFeeAccount;
import com.collegeerp.erp.fee.enums.PaymentStatus;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class FeeReceiptService {
    private final FeePaymentRepository payments;
    private final CollegeImageStorageService collegeImages;

    public FeeReceiptService(FeePaymentRepository payments,
            CollegeImageStorageService collegeImages) {
        this.payments = payments;
        this.collegeImages = collegeImages;
    }

    public FeeReceiptResponse currentStudentReceipt(Long paymentId) {
        FeePayment payment = payment(paymentId);
        if (!payment.getStudentUser().getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Payment receipt is outside your account");
        }
        return receipt(payment);
    }

    public FeeReceiptResponse collegeStaffReceipt(Long paymentId) {
        CustomUserDetails current = SecurityUtils.requireCurrentUser();
        boolean allowedRole = SecurityUtils.hasRole("FEE_SECTION")
                || SecurityUtils.hasRole("PRINCIPAL")
                || SecurityUtils.hasRole("SUPER_ADMIN");
        if (!allowedRole) {
            throw new AccessDeniedException("Fee receipt access is not permitted");
        }
        FeePayment payment = payment(paymentId);
        if (!SecurityUtils.hasRole("SUPER_ADMIN")
                && (current.getCollegeId() == null
                || !current.getCollegeId().equals(payment.getCollege().getId()))) {
            throw new AccessDeniedException("Payment receipt is outside your college");
        }
        return receipt(payment);
    }

    private FeePayment payment(Long paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private FeeReceiptResponse receipt(FeePayment payment) {
        if (payment.getStatus() != PaymentStatus.VERIFIED || payment.getVerifiedAt() == null) {
            throw new BadRequestException(
                    "An official receipt is available only after payment verification");
        }
        StudentFeeAccount account = payment.getStudentFeeAccount();
        College college = payment.getCollege();
        boolean provisional = account.getFeeStructure() == null;
        String courseYear = account.getFeeStructure() == null
                ? account.getAdmissionForm().getCourseYear() == null
                        ? "Admission"
                        : account.getAdmissionForm().getCourseYear().getName()
                : account.getFeeStructure().getCourseYear();
        String courseName = joinCourse(payment.getDepartment().getName(), courseYear);
        String remark = provisional
                ? "This receipt confirms the verified admission form fee payment. Admission remains subject to the selected college's admission rules and seat availability."
                : "This receipt confirms the verified fee payment recorded by the selected college.";
        LocalDate issuedOn = payment.getVerifiedAt().toLocalDate();

        return new FeeReceiptResponse(
                payment.getId(),
                receiptNumber(payment),
                "Fee Receipt",
                issuedOn,
                college.getId(),
                college.getCode(),
                college.getName(),
                collegeImages.publicLogoUrl(college.getCode(), college.getLogoUrl()),
                college.getAddress(),
                college.getCity(),
                college.getState(),
                college.getPincode(),
                college.getContactEmail(),
                college.getContactPhone(),
                payment.getStudent().getFullName(),
                payment.getStudent().getAdmissionNumber(),
                payment.getStudent().getPrn(),
                payment.getDepartment().getName(),
                courseName,
                account.getAcademicYear(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMode(),
                payment.getTransactionReference(),
                payment.getVerifiedAt(),
                payment.getVerifiedBy() == null ? null : payment.getVerifiedBy().getFullName(),
                remark);
    }

    private String receiptNumber(FeePayment payment) {
        return "RCP-" + payment.getCollege().getCode() + "-"
                + payment.getPaymentDate().getYear() + "-"
                + String.format("%06d", payment.getId());
    }

    private String joinCourse(String department, String courseYear) {
        if (courseYear == null || courseYear.isBlank()) return department;
        return department + " - " + courseYear.trim();
    }
}
