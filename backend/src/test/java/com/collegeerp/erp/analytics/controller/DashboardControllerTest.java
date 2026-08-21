package com.collegeerp.erp.analytics.controller;

import com.collegeerp.erp.academic.repository.AcademicClassRepository;
import com.collegeerp.erp.academic.repository.AttendanceSessionRepository;
import com.collegeerp.erp.academic.repository.SectionRepository;
import com.collegeerp.erp.academic.repository.SubjectRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.fee.dto.FeeBalanceTotals;
import com.collegeerp.erp.fee.repository.FeePaymentRepository;
import com.collegeerp.erp.fee.repository.StudentFeeAccountRepository;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {
    @Mock private CollegeRepository colleges;
    @Mock private DepartmentRepository departments;
    @Mock private UserRepository users;
    @Mock private StudentProfileRepository students;
    @Mock private StaffProfileRepository staff;
    @Mock private AdmissionFormRepository admissions;
    @Mock private StudentFeeAccountRepository fees;
    @Mock private FeePaymentRepository payments;
    @Mock private AcademicClassRepository classes;
    @Mock private SectionRepository sections;
    @Mock private SubjectRepository subjects;
    @Mock private AttendanceSessionRepository attendance;
    @InjectMocks private DashboardController controller;

    @Test
    void superAdminUsesSingleFeeAggregateInsteadOfLoadingAccounts() {
        when(fees.balanceTotals()).thenReturn(new FeeBalanceTotals(
                new BigDecimal("125000.00"), new BigDecimal("45000.00")));

        var response = controller.superAdmin();
        @SuppressWarnings("unchecked")
        var result = (java.util.Map<String, Object>) response.data();

        assertEquals(new BigDecimal("125000.00"), result.get("totalFeeCollected"));
        assertEquals(new BigDecimal("45000.00"), result.get("totalFeePending"));
        verify(fees).balanceTotals();
        verify(fees, never()).findAll();
    }
}
