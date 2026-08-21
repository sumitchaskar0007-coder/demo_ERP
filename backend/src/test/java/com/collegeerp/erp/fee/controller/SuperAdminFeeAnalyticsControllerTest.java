package com.collegeerp.erp.fee.controller;

import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.analytics.repository.AdminAnalyticsReadRepository;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.fee.dto.FeeCollectionRow;
import com.collegeerp.erp.fee.dto.PendingFeeRow;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
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
import java.util.Map;

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
    @Mock private AdminAnalyticsReadRepository analyticsRead;

    private SuperAdminFeeAnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new SuperAdminFeeAnalyticsController(
                accounts, payments, colleges, users, staff, students, admissions, enrollments, analyticsRead);
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
        when(payments.sumVerifiedCollections(
                isNull(), isNull(), isNull(), isNull(), eq(""), isNull(), isNull()))
                .thenReturn(new BigDecimal("125000.00"));

        var response = controller.collections(null, null, "  ", null, null, null, null, 0, 20);

        assertEquals(101, response.data().totalElements());
        assertEquals(20, response.data().size());
        assertEquals(new BigDecimal("125000.00"), response.data().totalAmount());
        assertEquals("TXN-1", response.data().content().get(0).transactionReference());
        verify(payments, never()).findAll();
    }

    @Test
    void analyticsReturnsFilteredShape() {
        when(analyticsRead.read(null, null, null, null)).thenReturn(Map.of(
                "summary", Map.of("totalFeeCollection", BigDecimal.ZERO),
                "collegeWiseStudents", List.of(),
                "collegeWiseFeeCollection", List.of(),
                "admissionStatusDistribution", Map.of(),
                "pendingFees", List.of()));

        var response = controller.analytics(null, null, null, null);

        assertEquals(BigDecimal.ZERO,
                ((java.util.Map<?, ?>) response.data().get("summary")).get("totalFeeCollection"));
        verify(analyticsRead).read(null, null, null, null);
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
