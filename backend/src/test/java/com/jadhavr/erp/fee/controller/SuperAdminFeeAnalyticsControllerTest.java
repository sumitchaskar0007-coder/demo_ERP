package com.jadhavr.erp.fee.controller;

import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.dto.FeeCollectionRow;
import com.jadhavr.erp.fee.dto.PendingFeeRow;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminFeeAnalyticsControllerTest {

    @Mock private StudentFeeAccountRepository accounts;
    @Mock private FeePaymentRepository payments;
    @Mock private CollegeRepository colleges;
    @Mock private UserRepository users;
    @Mock private StaffProfileRepository staff;
    @Mock private StudentProfileRepository students;
    @Mock private AdmissionFormRepository admissions;
    @Mock private StudentSectionEnrollmentRepository enrollments;

    private SuperAdminFeeAnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new SuperAdminFeeAnalyticsController(
                accounts, payments, colleges, users, staff, students, admissions, enrollments);
    }

    @Test
    void collectionsUsesPagedProjectionQuery() {
        FeeCollectionRow row = new FeeCollectionRow(
                1L, "Student", "College", "Department", null, null, null,
                new BigDecimal("1000.00"), null, "TXN-1");
        when(payments.findVerifiedCollections(
                isNull(), isNull(), isNull(), isNull(), eq(""), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(row), PageRequest.of(0, 20), 101));

        var response = controller.collections(null, null, "  ", null, null, null, null, 0, 20);

        assertEquals(101, response.data().totalElements());
        assertEquals(20, response.data().size());
        assertEquals("TXN-1", response.data().content().get(0).transactionReference());
        verify(payments, never()).findAll();
    }

    @Test
    void analyticsReturnsFilteredShape() {
        when(payments.sumByStatus(com.jadhavr.erp.fee.enums.PaymentStatus.VERIFIED))
                .thenReturn(BigDecimal.ZERO);
        when(accounts.sumRemainingAmount()).thenReturn(new BigDecimal("500.00"));
        when(enrollments.findAll()).thenReturn(List.of());
        when(admissions.findAll()).thenReturn(List.of());
        when(students.findAll()).thenReturn(List.of());
        when(accounts.findAll()).thenReturn(List.of());
        when(staff.findAll()).thenReturn(List.of());

        when(accounts.findPendingFees(
                isNull(), isNull(), isNull(), isNull(), eq(""), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<PendingFeeRow>(List.of()));

        var response = controller.analytics(1L, null, null, null);

        assertEquals(BigDecimal.ZERO,
                ((java.util.Map<?, ?>) response.data().get("summary")).get("totalFeeCollection"));
    }

    @Test
    void analyticsUsesAggregateQueriesInsteadOfFullTableReads() {
        when(payments.sumByStatus(com.jadhavr.erp.fee.enums.PaymentStatus.VERIFIED))
                .thenReturn(new BigDecimal("1500.00"));
        when(accounts.sumRemainingAmount()).thenReturn(new BigDecimal("500.00"));
        when(payments.sumVerifiedByCollege()).thenReturn(List.of());
        when(students.countStudentsByCollege()).thenReturn(List.of());
        when(admissions.countAdmissionsByStatus()).thenReturn(List.of());
        when(accounts.findPendingFees(
                isNull(), isNull(), isNull(), isNull(), eq(""), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<PendingFeeRow>(List.of()));

        var response = controller.analytics(null, null, null, null);

        assertEquals(new BigDecimal("1500.00"),
                ((java.util.Map<?, ?>) response.data().get("summary")).get("totalFeeCollection"));
        verify(enrollments, never()).findAll();
        verify(admissions, never()).findAll();
        verify(students, never()).findAll();
        verify(accounts, never()).findAll();
        verify(payments, never()).findAll();
        verify(colleges, never()).findAll();
        verify(users, never()).findAll();
    }

    @Test
    void rejectsUnboundedPageSizes() {
        assertThrows(BadRequestException.class,
                () -> controller.pending(null, null, null, null, null, null, null, 0, 101));
    }
}
