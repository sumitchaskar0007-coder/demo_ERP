package com.collegeerp.erp.fee.service;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.service.CollegeImageStorageService;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.fee.dto.FeeReceiptResponse;
import com.collegeerp.erp.fee.entity.FeePayment;
import com.collegeerp.erp.fee.entity.StudentFeeAccount;
import com.collegeerp.erp.fee.enums.PaymentMode;
import com.collegeerp.erp.fee.enums.PaymentStatus;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeReceiptServiceTest {
    @Mock private FeePaymentRepository payments;
    @Mock private CollegeImageStorageService collegeImages;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentReceiptMapsVerifiedPaymentAndCollegeBranding() {
        FeePayment payment = payment(PaymentStatus.VERIFIED);
        authenticate(RoleName.STUDENT, 25L, 7L);
        when(payments.findById(31L)).thenReturn(Optional.of(payment));
        when(collegeImages.publicLogoUrl("AIMS", "colleges/7/logo.png"))
                .thenReturn("/api/public/admissions/college/AIMS/logo");

        FeeReceiptResponse receipt = service().currentStudentReceipt(31L);

        assertEquals("RCP-AIMS-2026-000031", receipt.receiptNumber());
        assertEquals("Fee Receipt", receipt.receiptTitle());
        assertEquals("Aditya Institute - First Year", receipt.courseName());
        assertEquals("UTR123456789012", receipt.transactionReference());
        assertEquals("/api/public/admissions/college/AIMS/logo", receipt.collegeLogoUrl());
        assertEquals(LocalDate.of(2026, 6, 24), receipt.issuedOn());
    }

    @Test
    void studentCannotReadAnotherStudentsReceipt() {
        authenticate(RoleName.STUDENT, 99L, 7L);
        when(payments.findById(31L)).thenReturn(Optional.of(payment(PaymentStatus.VERIFIED)));

        assertThrows(AccessDeniedException.class,
                () -> service().currentStudentReceipt(31L));
    }

    @Test
    void receiptIsUnavailableUntilPaymentIsVerified() {
        authenticate(RoleName.STUDENT, 25L, 7L);
        when(payments.findById(31L)).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));

        assertThrows(BadRequestException.class,
                () -> service().currentStudentReceipt(31L));
    }

    @Test
    void feeOfficerCannotReadAnotherCollegesReceipt() {
        authenticate(RoleName.FEE_SECTION, 40L, 8L);
        when(payments.findById(31L)).thenReturn(Optional.of(payment(PaymentStatus.VERIFIED)));

        assertThrows(AccessDeniedException.class,
                () -> service().collegeStaffReceipt(31L));
    }

    private FeeReceiptService service() {
        return new FeeReceiptService(payments, collegeImages);
    }

    private FeePayment payment(PaymentStatus status) {
        College college = new College();
        college.setId(7L);
        college.setCode("AIMS");
        college.setName("Aditya Institute");
        college.setLogoUrl("colleges/7/logo.png");
        college.setAddress("Narhe");
        college.setCity("Pune");
        college.setState("Maharashtra");
        college.setPincode("411041");
        college.setContactEmail("admission@example.test");
        college.setContactPhone("9356399629");

        Department department = new Department();
        department.setId(11L);
        department.setName("Aditya Institute");
        department.setCollege(college);

        User studentUser = new User();
        studentUser.setId(25L);
        StudentProfile student = new StudentProfile();
        student.setId(12L);
        student.setUser(studentUser);
        student.setFullName("Test Student");
        student.setAdmissionNumber("AIMS-2026-001");
        student.setPrn("PRN-1001");

        com.collegeerp.erp.academic.entity.AcademicClass courseYear =
                new com.collegeerp.erp.academic.entity.AcademicClass();
        courseYear.setName("First Year");
        AdmissionForm admission = new AdmissionForm();
        admission.setCourseYear(courseYear);

        StudentFeeAccount account = new StudentFeeAccount();
        account.setAdmissionForm(admission);
        account.setAcademicYear("2026-2027");

        User verifier = new User();
        verifier.setFullName("Fee Officer");

        FeePayment payment = new FeePayment();
        payment.setId(31L);
        payment.setStudentFeeAccount(account);
        payment.setStudent(student);
        payment.setStudentUser(studentUser);
        payment.setCollege(college);
        payment.setDepartment(department);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentMode(PaymentMode.UPI);
        payment.setTransactionReference("UTR123456789012");
        payment.setPaymentDate(LocalDate.of(2026, 6, 5));
        payment.setStatus(status);
        payment.setVerifiedAt(status == PaymentStatus.VERIFIED
                ? LocalDateTime.of(2026, 6, 24, 10, 30) : null);
        payment.setVerifiedBy(status == PaymentStatus.VERIFIED ? verifier : null);
        return payment;
    }

    private void authenticate(RoleName role, Long userId, Long collegeId) {
        CustomUserDetails details = TestSecurityUsers.details(role, userId, collegeId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        details, null, details.getAuthorities()));
    }
}
