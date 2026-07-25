package com.jadhavr.erp.reports.controller;

import com.jadhavr.erp.academic.repository.AttendanceRecordRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.reports.dto.AttendanceReportRow;
import com.jadhavr.erp.reports.service.AdmissionAnalyticsService;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerSecurityTest {
    @Mock private AdmissionFormRepository admissions;
    @Mock private StudentFeeAccountRepository fees;
    @Mock private StudentProfileRepository students;
    @Mock private AttendanceRecordRepository attendance;
    @Mock private StaffProfileRepository staff;
    @Mock private AuditLogService audit;
    @Mock private AdmissionAnalyticsService admissionAnalytics;
    @InjectMocks private ReportController controller;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCannotExportCollegeReports() {
        authenticate(RoleName.STUDENT, 8L, 10L);
        assertThrows(AccessDeniedException.class,
                () -> controller.export("students", null, null, null));
    }

    @Test
    void principalCannotRequestAnotherCollege() {
        authenticate(RoleName.PRINCIPAL, 2L, 10L);
        assertThrows(AccessDeniedException.class,
                () -> controller.students(99L, null, null, 0, 20));
    }

    @Test
    void hodCannotRequestAnotherDepartment() {
        authenticate(RoleName.HOD, 5L, 10L);
        StaffProfile hod = org.mockito.Mockito.mock(StaffProfile.class);
        when(hod.getDepartment()).thenReturn(org.mockito.Mockito.mock(com.jadhavr.erp.department.entity.Department.class));
        when(hod.getDepartment().getId()).thenReturn(20L);
        when(hod.belongsToDepartment(99L)).thenReturn(false);
        when(staff.findByUserId(5L)).thenReturn(Optional.of(hod));

        assertThrows(AccessDeniedException.class,
                () -> controller.attendance(10L, 99L, 0, 20));
    }

    @Test
    void attendanceExportContainsAttendanceAndNeutralizesFormulaCells() {
        authenticate(RoleName.SUPER_ADMIN, 1L, null);
        AttendanceReportRow row = row("=HYPERLINK(\"https://example.test\")", "-1", 10, 8);
        when(attendance.attendanceReport(isNull(), isNull(), any(Pageable.class))).thenReturn(List.of(row));

        byte[] body = controller.export("attendance", null, null, null).getBody();
        String csv = new String(body, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("Student,Roll Number,Total Sessions,Present,Absent,Attendance Percentage"));
        assertTrue(csv.contains("\"'=HYPERLINK"));
        assertTrue(csv.contains("\"'-1\""));
        assertTrue(csv.contains(",\"10\",\"8\",\"2\",\"80.00\""));
    }

    @Test
    void oversizedExportIsRejected() {
        authenticate(RoleName.SUPER_ADMIN, 1L, null);
        AttendanceReportRow row = row("Student", "1", 1, 1);
        when(attendance.attendanceReport(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Collections.nCopies(10_001, row));

        assertThrows(BadRequestException.class,
                () -> controller.export("attendance", null, null, null));
    }

    private void authenticate(RoleName role, Long userId, Long collegeId) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, userId, collegeId));
    }

    private AttendanceReportRow row(String name, String roll, long total, long present) {
        return new AttendanceReportRow() {
            public String getStudentName() { return name; }
            public String getRollNumber() { return roll; }
            public long getTotalSessions() { return total; }
            public long getPresentCount() { return present; }
        };
    }
}
