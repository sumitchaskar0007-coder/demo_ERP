package com.jadhavr.erp.audit.service;

import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.jadhavr.erp.audit.enums.AuditAction;
import com.jadhavr.erp.audit.enums.AuditModule;
import com.jadhavr.erp.audit.repository.AuditLogRepository;
import com.jadhavr.erp.auth.repository.SecurityAuditEventRepository;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceReportTransactionTest {
    @Mock private AuditLogRepository repo;
    @Mock private UserRepository users;
    @Mock private StaffProfileRepository staff;
    @Mock private AdmissionFormRepository admissions;
    @Mock private WeeklyAttendanceSessionRepository attendanceSessions;
    @Mock private SecurityAuditEventRepository securityEvents;
    @Mock private WeeklyTimetableEntryRepository timetableEntries;
    @InjectMocks private AuditLogService service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reportAuditMethodsRequireIndependentWritableTransactions() throws Exception {
        assertRequiresNew("logReportView");
        assertRequiresNew("logReportExport");
    }

    @Test
    void reportViewPersistsTheExpectedAuditRecord() {
        authenticate();
        when(users.findById(99L)).thenReturn(Optional.empty());

        service.logReportView("Admission Analytics");

        verify(repo).save(argThat(row ->
                row.getModule() == AuditModule.REPORT
                        && row.getAction() == AuditAction.VIEW_REPORT
                        && "Report".equals(row.getEntityType())
                        && "Viewed Admission Analytics report".equals(row.getDescription())));
    }

    @Test
    void reportExportPersistsTheExpectedAuditRecord() {
        authenticate();
        when(users.findById(99L)).thenReturn(Optional.empty());

        service.logReportExport("admissions");

        verify(repo).save(argThat(row ->
                row.getModule() == AuditModule.REPORT
                        && row.getAction() == AuditAction.EXPORT
                        && "Generated and downloaded admissions report".equals(row.getDescription())));
    }

    private void assertRequiresNew(String methodName) throws Exception {
        Method method = AuditLogService.class.getMethod(methodName, String.class);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.PRINCIPAL, 99L, 10L));
    }
}
