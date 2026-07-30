package com.jadhavr.erp.audit.service;

import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.jadhavr.erp.audit.entity.AuditLog;
import com.jadhavr.erp.audit.repository.AuditLogRepository;
import com.jadhavr.erp.auth.repository.SecurityAuditEventRepository;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceQueryBoundTest {

    @Mock private AuditLogRepository repo;
    @Mock private UserRepository users;
    @Mock private StaffProfileRepository staff;
    @Mock private AdmissionFormRepository admissions;
    @Mock private WeeklyAttendanceSessionRepository attendanceSessions;
    @Mock private SecurityAuditEventRepository securityEvents;
    @Mock private WeeklyTimetableEntryRepository timetableEntries;
    @InjectMocks private AuditLogService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.PRINCIPAL, 99L, 10L));
        when(staff.findByCollegeId(10L)).thenReturn(List.of());
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    return new PageImpl<AuditLog>(List.of(), pageable, 6_001);
                });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardCapsInMemoryAnalyticsAndReportsTheCap() {
        var response = service.dashboard(
                null, null, null, null, null, null,
                null, null, null, 0, 25, null);

        verify(repo).findAll(any(Specification.class),
                argThat((Pageable pageable) -> pageable.getPageSize() == 5_000));
        assertTrue(response.insights().stream()
                .anyMatch(insight -> insight.contains("latest 5000 matching audit events")));
    }
}
