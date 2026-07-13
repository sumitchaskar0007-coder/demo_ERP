package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.fee.dto.VerifyPaymentRequest;
import com.jadhavr.erp.fee.entity.FeePayment;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.fee.enums.FeeStructureStatus;
import com.jadhavr.erp.fee.enums.PaymentStatus;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.FeeStructureRepository;
import com.jadhavr.erp.fee.repository.FeeTransactionRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeServiceImplTest {
    @Mock private FeeStructureRepository structures;
    @Mock private StudentFeeAccountRepository accounts;
    @Mock private FeePaymentRepository payments;
    @Mock private FeeTransactionRepository transactions;
    @Mock private CollegeRepository colleges;
    @Mock private DepartmentRepository departments;
    @Mock private UserRepository users;
    @Mock private AdmissionFormRepository admissions;
    @Mock private AdmissionStatusHistoryRepository histories;

    private FeeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FeeServiceImpl(structures, accounts, payments, transactions,
                colleges, departments, users, admissions, histories);
        authenticateSuperAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void feeAccountUsesVerifiedStudentCategoryAndPreservesSnapshot() {
        AdmissionForm admission = admission(StudentCategory.SC);
        FeeStructure structure = new FeeStructure();
        structure.setStudentCategory(StudentCategory.SC);
        structure.setTotalFee(new BigDecimal("12000.00"));
        structure.setMinimumAmountForAdmission(new BigDecimal("2000.00"));
        when(accounts.existsByAdmissionFormId(40L)).thenReturn(false);
        when(structures.findFirstByCollegeIdAndDepartmentIdAndAcademicYearInAndStudentCategoryAndStatus(
                1L, 10L, List.of("2026-2027", "2026-27"), StudentCategory.SC, FeeStructureStatus.ACTIVE))
                .thenReturn(Optional.of(structure));

        service.createAccountForAdmission(admission);

        ArgumentCaptor<StudentFeeAccount> captor = ArgumentCaptor.forClass(StudentFeeAccount.class);
        verify(accounts).save(captor.capture());
        assertEquals(StudentCategory.SC, captor.getValue().getStudentCategory());
        assertSame(structure, captor.getValue().getFeeStructure());
        assertEquals(new BigDecimal("12000.00"), captor.getValue().getRemainingAmount());
    }

    @Test
    void verificationLocksPaymentAndAccountAndRejectsOverpayment() {
        StudentFeeAccount account = new StudentFeeAccount();
        account.setId(20L);
        account.setTotalFee(new BigDecimal("1000.00"));
        account.setPaidAmount(new BigDecimal("600.00"));
        account.setRemainingAmount(new BigDecimal("400.00"));
        FeePayment payment = payment(account, new BigDecimal("500.00"), PaymentStatus.PENDING);
        when(payments.findByIdForUpdate(30L)).thenReturn(Optional.of(payment));
        when(accounts.findByIdForUpdate(20L)).thenReturn(Optional.of(account));
        when(transactions.existsByFeePaymentId(30L)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.verify(30L, new VerifyPaymentRequest("checked")));

        verify(payments).findByIdForUpdate(30L);
        verify(accounts).findByIdForUpdate(20L);
        verify(payments, never()).save(any());
        verify(accounts, never()).save(any());
        verify(transactions, never()).save(any());
    }

    @Test
    void alreadyVerifiedPaymentIsIdempotentlyRejectedAfterLock() {
        StudentFeeAccount account = new StudentFeeAccount();
        account.setId(20L);
        FeePayment payment = payment(account, new BigDecimal("100.00"), PaymentStatus.VERIFIED);
        when(payments.findByIdForUpdate(30L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> service.verify(30L, new VerifyPaymentRequest(null)));

        verify(payments).findByIdForUpdate(30L);
        verify(accounts, never()).findByIdForUpdate(any());
    }

    private AdmissionForm admission(StudentCategory category) {
        College college = college();
        Department department = new Department();
        department.setId(10L);
        department.setCollege(college);
        StudentProfile student = new StudentProfile();
        student.setId(2L);
        User studentUser = new User();
        studentUser.setId(3L);
        AdmissionForm admission = new AdmissionForm();
        admission.setId(40L);
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(student);
        admission.setStudentUser(studentUser);
        admission.setAcademicYear("2026-2027");
        admission.setStudentCategory(category);
        return admission;
    }

    private FeePayment payment(StudentFeeAccount account, BigDecimal amount, PaymentStatus status) {
        FeePayment payment = new FeePayment();
        payment.setId(30L);
        payment.setStudentFeeAccount(account);
        payment.setCollege(college());
        payment.setAmount(amount);
        payment.setStatus(status);
        return payment;
    }

    private College college() {
        College college = new College();
        college.setId(1L);
        return college;
    }

    private void authenticateSuperAdmin() {
        Role role = new Role();
        role.setName(RoleName.SUPER_ADMIN);
        User user = new User();
        user.setId(99L);
        user.setFullName("Super Admin");
        user.setEmail("admin@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
