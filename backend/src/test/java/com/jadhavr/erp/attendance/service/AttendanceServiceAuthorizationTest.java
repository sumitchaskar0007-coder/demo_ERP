package com.jadhavr.erp.attendance.service;

import com.jadhavr.erp.attendance.dto.AttendanceDtos.MarkItem;
import com.jadhavr.erp.attendance.dto.AttendanceDtos.SubmitRequest;
import com.jadhavr.erp.attendance.entity.AttendanceModels.AttendanceSession;
import com.jadhavr.erp.attendance.repository.AttendanceCorrectionRepository;
import com.jadhavr.erp.attendance.repository.AttendanceSessionRepository;
import com.jadhavr.erp.attendance.repository.AuditLogRepository;
import com.jadhavr.erp.attendance.repository.StudentAttendanceRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceAuthorizationTest {
    @Mock private EntityManager entityManager;
    @Mock private AttendanceSessionRepository sessions;
    @Mock private StudentAttendanceRepository attendance;
    @Mock private AttendanceCorrectionRepository corrections;
    @Mock private AuditLogRepository audits;
    private AttendanceService service;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(entityManager, sessions, attendance, corrections, audits, 75);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCannotReadAnotherStudentsAttendance() {
        authenticate(RoleName.STUDENT, 8L, 10L);
        StudentProfile otherStudent = student(44L, 9L, 10L);
        when(entityManager.find(StudentProfile.class, 44L)).thenReturn(otherStudent);

        assertThrows(AccessDeniedException.class,
                () -> service.report(44L, LocalDate.now().minusDays(7), LocalDate.now()));
        verify(attendance, never()).report(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unassignedTeacherCannotSubmitAndNoRecordIsChanged() {
        authenticate(RoleName.SUBJECT_TEACHER, 7L, 10L);
        AttendanceSession session = org.mockito.Mockito.mock(AttendanceSession.class);
        when(session.getSessionDate()).thenReturn(LocalDate.now());
        User assignedTeacher = new User();
        assignedTeacher.setId(99L);
        when(session.getAssignedTeacher()).thenReturn(assignedTeacher);
        when(sessions.findByIdAndCollegeId(55L, 10L)).thenReturn(Optional.of(session));

        SubmitRequest request = new SubmitRequest(List.of(new MarkItem(44L, "PRESENT", null)));
        assertThrows(AccessDeniedException.class, () -> service.submit(55L, request));

        verify(session, never()).setStatus(org.mockito.ArgumentMatchers.any());
        verify(attendance, never()).save(org.mockito.ArgumentMatchers.any());
        verify(sessions, never()).save(org.mockito.ArgumentMatchers.any());
        verify(audits, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void authenticate(RoleName role, Long userId, Long collegeId) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, userId, collegeId));
    }

    private StudentProfile student(Long id, Long userId, Long collegeId) {
        StudentProfile profile = new StudentProfile();
        profile.setId(id);
        profile.setCollege(college(collegeId));
        User user = new User();
        user.setId(userId);
        profile.setUser(user);
        return profile;
    }

    private College college(Long id) {
        College college = new College();
        college.setId(id);
        return college;
    }
}
