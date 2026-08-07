package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.entity.StudentSectionEnrollment;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.AuthorizationSnapshot;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.dto.ChangeStudentCategoryRequest;
import com.jadhavr.erp.fee.dto.RemoveScholarshipRequest;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.entity.FeeTransaction;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.fee.enums.FeeStructureStatus;
import com.jadhavr.erp.fee.enums.FeeTransactionType;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.fee.repository.FeeStructureRepository;
import com.jadhavr.erp.fee.repository.FeeTransactionRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.notice.service.NoticeService;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrincipalFeeAdjustmentServiceTest {
    @Mock StudentFeeAccountRepository accounts;
    @Mock FeeStructureRepository structures;
    @Mock FeeTransactionRepository transactions;
    @Mock AdmissionFormRepository admissions;
    @Mock StudentSectionEnrollmentRepository enrollments;
    @Mock UserRepository users;
    @Mock NoticeService notices;

    private PrincipalFeeAdjustmentService service;

    @BeforeEach
    void setUp() {
        service = new PrincipalFeeAdjustmentService(
                accounts, structures, transactions, admissions, enrollments, users, notices);
        CustomUserDetails principal = new CustomUserDetails(new AuthorizationSnapshot(
                9L, 1L, "principal@example.test", UserStatus.ACTIVE,
                null, 0, List.of("ROLE_PRINCIPAL")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void removingScholarshipPreservesPaymentAndRaisesRemainingBalance() {
        StudentFeeAccount account = account("MALE");
        account.setTotalFee(money("80000"));
        account.setDiscountAmount(money("50000"));
        account.setPaidAmount(money("30000"));
        account.setRemainingAmount(money("0"));
        stubLocked(account);

        var result = service.removeScholarship(
                20L, new RemoveScholarshipRequest("Eligibility withdrawn"));

        assertThat(result.scholarshipAmount()).isEqualByComparingTo("0.00");
        assertThat(result.remainingAmount()).isEqualByComparingTo("50000.00");
        assertThat(account.getPaidAmount()).isEqualByComparingTo("30000.00");
        assertThat(account.isScholarshipRemoved()).isTrue();
        ArgumentCaptor<FeeTransaction> transaction = ArgumentCaptor.forClass(FeeTransaction.class);
        verify(transactions).save(transaction.capture());
        assertThat(transaction.getValue().getTransactionType())
                .isEqualTo(FeeTransactionType.SCHOLARSHIP_REMOVED);
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("50000.00");
    }

    @ParameterizedTest
    @CsvSource({"MALE,50000,0", "FEMALE,30000,20000"})
    void categoryChangeUsesOnlyTheExactGenderAssessment(
            String gender, String scholarship, String expectedRemaining) {
        StudentFeeAccount account = account(gender);
        account.setTotalFee(money("80000"));
        account.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        account.setPaidAmount(money("30000"));
        account.setRemainingAmount(money("50000"));
        stubLocked(account);

        FeeStructure target = new FeeStructure();
        target.setStudentCategory(StudentCategory.OBC);
        target.setGender(gender);
        target.setTotalFee(money("80000"));
        target.setScholarshipAmount(money(scholarship));
        target.setMinimumAmountForAdmission(money("10000"));
        when(structures.findConfiguredAssessments(
                1L, 2L, List.of("2026-2027", "2026-27"), StudentCategory.OBC,
                null, gender, "First Year", FeeStructureStatus.ACTIVE))
                .thenReturn(List.of(target));

        var result = service.changeCategory(20L, new ChangeStudentCategoryRequest(
                StudentCategory.OBC, null, "Kunbi", "Certificate verified"));

        assertThat(result.scholarshipAmount()).isEqualByComparingTo(scholarship);
        assertThat(result.remainingAmount()).isEqualByComparingTo(expectedRemaining);
        assertThat(account.getAdmissionForm().getStudentCategory()).isEqualTo(StudentCategory.OBC);
        assertThat(account.getStudent().getStudentCategory()).isEqualTo(StudentCategory.OBC);
        assertThat(account.getAdmissionForm().getCaste()).isEqualTo("Kunbi");
        verify(structures).findConfiguredAssessments(
                1L, 2L, List.of("2026-2027", "2026-27"), StudentCategory.OBC,
                null, gender, "First Year", FeeStructureStatus.ACTIVE);
    }

    @Test
    void categoryChangeRecordsCreditWhenVerifiedPaymentsExceedNewPayable() {
        StudentFeeAccount account = account("FEMALE");
        account.setTotalFee(money("80000"));
        account.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        account.setPaidAmount(money("70000"));
        account.setRemainingAmount(money("10000"));
        stubLocked(account);

        FeeStructure target = new FeeStructure();
        target.setStudentCategory(StudentCategory.SC);
        target.setGender("FEMALE");
        target.setTotalFee(money("80000"));
        target.setScholarshipAmount(money("30000"));
        target.setMinimumAmountForAdmission(money("10000"));
        when(structures.findConfiguredAssessments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(target));

        var result = service.changeCategory(20L, new ChangeStudentCategoryRequest(
                StudentCategory.SC, null, "Scheduled Caste", "Certificate corrected"));

        assertThat(result.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(result.creditAmount()).isEqualByComparingTo("20000.00");
    }

    @Test
    void categoryChangeDoesNotRestoreScholarshipRemovedByPrincipal() {
        StudentFeeAccount account = account("FEMALE");
        account.setTotalFee(money("80000"));
        account.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        account.setPaidAmount(money("30000"));
        account.setRemainingAmount(money("50000"));
        account.setScholarshipRemoved(true);
        stubLocked(account);

        FeeStructure target = new FeeStructure();
        target.setStudentCategory(StudentCategory.OBC);
        target.setGender("FEMALE");
        target.setTotalFee(money("80000"));
        target.setScholarshipAmount(money("30000"));
        target.setMinimumAmountForAdmission(money("10000"));
        when(structures.findConfiguredAssessments(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(target));

        var result = service.changeCategory(20L, new ChangeStudentCategoryRequest(
                StudentCategory.OBC, null, "Kunbi", "Certificate corrected"));

        assertThat(result.scholarshipAmount()).isEqualByComparingTo("0.00");
        assertThat(result.remainingAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.scholarshipRemoved()).isTrue();
    }

    @Test
    void legacyAdmissionUsesActiveEnrollmentCourseYearForCategoryOptions() {
        StudentFeeAccount account = account("FEMALE");
        account.getAdmissionForm().setCourseYear(null);
        when(accounts.findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(account));
        AcademicClass secondYear = new AcademicClass();
        secondYear.setName("Second Year");
        StudentSectionEnrollment enrollment = new StudentSectionEnrollment();
        enrollment.setAcademicClass(secondYear);
        when(enrollments.findByStudentIdAndAcademicYearAndStatus(
                20L, "2026-2027", AcademicStatus.ACTIVE)).thenReturn(Optional.of(enrollment));
        when(structures.findAssessmentOptions(
                1L, 2L, List.of("2026-2027", "2026-27"), "FEMALE", "Second Year"))
                .thenReturn(List.of());

        assertThat(service.categoryOptions(20L)).isEmpty();

        verify(structures).findAssessmentOptions(
                1L, 2L, List.of("2026-2027", "2026-27"), "FEMALE", "Second Year");
    }

    @Test
    void superAdminCannotUsePrincipalOnlyFeeAdjustment() {
        authenticate(9L, null, "ROLE_SUPER_ADMIN");

        assertThatThrownBy(() -> service.categoryOptions(20L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("Only Principal can adjust student fees");
        verifyNoInteractions(accounts);
    }

    @Test
    void principalCannotAdjustStudentOutsideOwnCollege() {
        StudentFeeAccount account = account("MALE");
        when(accounts.findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(account));
        authenticate(9L, 99L, "ROLE_PRINCIPAL");

        assertThatThrownBy(() -> service.categoryOptions(20L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("Student is outside your college");
    }

    private void stubLocked(StudentFeeAccount account) {
        when(accounts.findFirstByStudentIdAndFeeStructureIsNotNullOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(30L)).thenReturn(Optional.of(account));
        User principal = new User();
        principal.setId(9L);
        principal.setFullName("Principal");
        when(users.findById(9L)).thenReturn(Optional.of(principal));
    }

    private StudentFeeAccount account(String gender) {
        College college = new College();
        college.setId(1L);
        college.setName("College");
        college.setCode("COL");
        Department department = new Department();
        department.setId(2L);
        department.setName("Science");
        department.setCode("SCI");
        department.setCollege(college);
        AcademicClass courseYear = new AcademicClass();
        courseYear.setId(3L);
        courseYear.setName("First Year");

        User studentUser = new User();
        studentUser.setId(10L);
        StudentProfile student = new StudentProfile();
        student.setId(20L);
        student.setFullName("Student");
        student.setAdmissionNumber("ADM-20");
        student.setStudentCategory(StudentCategory.OPEN);
        AdmissionForm admission = new AdmissionForm();
        admission.setId(40L);
        admission.setAdmissionReferenceNumber("REF-40");
        admission.setStudent(student);
        admission.setStudentUser(studentUser);
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setAcademicYear("2026-2027");
        admission.setCourseYear(courseYear);
        admission.setGender(gender);
        admission.setStudentCategory(StudentCategory.OPEN);

        FeeStructure current = new FeeStructure();
        current.setMinimumAmountForAdmission(money("10000"));
        StudentFeeAccount account = new StudentFeeAccount();
        account.setId(30L);
        account.setStudent(student);
        account.setStudentUser(studentUser);
        account.setAdmissionForm(admission);
        account.setCollege(college);
        account.setDepartment(department);
        account.setAcademicYear("2026-2027");
        account.setStudentCategory(StudentCategory.OPEN);
        account.setFeeStructure(current);
        account.setCreditAmount(BigDecimal.ZERO.setScale(2));
        return account;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private void authenticate(Long userId, Long collegeId, String role) {
        CustomUserDetails details = new CustomUserDetails(new AuthorizationSnapshot(
                userId, collegeId, "actor@example.test", UserStatus.ACTIVE,
                null, 0, List.of(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        details, null, details.getAuthorities()));
    }
}
