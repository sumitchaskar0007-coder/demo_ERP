package com.collegeerp.erp.attendance.service;

import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.attendance.repository.AuditLogRepository;
import com.collegeerp.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.collegeerp.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.service.EffectiveLectureService;
import com.collegeerp.erp.user.entity.RoleName;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyAttendanceServiceQueryScopeTest {

    @Mock private WeeklyTimetableEntryRepository entries;
    @Mock private WeeklyAttendanceSessionRepository sessions;
    @Mock private WeeklyAttendanceRecordRepository records;
    @Mock private StudentSectionEnrollmentRepository enrollments;
    @Mock private StaffProfileRepository staff;
    @Mock private StudentProfileRepository students;
    @Mock private AuditLogRepository audits;
    @Mock private EffectiveLectureService effectiveLectures;
    private WeeklyAttendanceService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyAttendanceService(
                entries, sessions, records, enrollments, staff, students, audits, effectiveLectures,
                0, "PRESENT");
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.PRINCIPAL, 99L, 10L));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalReportQueriesOnlyCurrentCollegeEnrollmentAndTimetableScope() {
        LocalDate day = LocalDate.of(2026, 7, 28);
        when(sessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(
                10L, day, day)).thenReturn(List.of());
        when(enrollments.findForAttendanceReport(
                AcademicStatus.ACTIVE, 10L, null, null, null)).thenReturn(List.of());
        when(entries.findApprovedForAttendanceReport(
                10L, null, null, null, null, null)).thenReturn(List.of());

        var report = service.principalReport(day, day, null, null, null, null);

        assertEquals(0, report.sessions());
        verify(enrollments).findForAttendanceReport(
                AcademicStatus.ACTIVE, 10L, null, null, null);
        verify(entries).findApprovedForAttendanceReport(
                10L, null, null, null, null, null);
    }
}
