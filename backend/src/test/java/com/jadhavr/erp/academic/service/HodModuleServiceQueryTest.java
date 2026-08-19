package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.SubjectStatus;
import com.jadhavr.erp.academic.enums.SemesterOfferingStatus;
import com.jadhavr.erp.academic.repository.ClassTeacherAssignmentHistoryRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.StudentDivisionTransferRepository;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.academic.repository.SubjectRepository;
import com.jadhavr.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceRecord;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceSession;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.jadhavr.erp.audit.service.AuditLogService;
import com.jadhavr.erp.auth.security.AuthorizationSnapshotService;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.teacher.service.TeacherNotificationService;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HodModuleServiceQueryTest {

    @Mock private StudentProfileRepository students;
    @Mock private StaffProfileRepository staff;
    @Mock private SectionRepository sections;
    @Mock private SubjectRepository subjects;
    @Mock private StudentSectionEnrollmentRepository enrollments;
    @Mock private SubjectTeacherAssignmentRepository subjectAssignments;
    @Mock private WeeklyTimetableRepository timetables;
    @Mock private WeeklyTimetableEntryRepository timetableEntries;
    @Mock private WeeklyAttendanceRecordRepository attendance;
    @Mock private StudentDivisionTransferRepository transfers;
    @Mock private ClassTeacherAssignmentHistoryRepository classTeacherHistory;
    @Mock private AuditLogService audit;
    @Mock private TeacherNotificationService notifications;
    @Mock private AdmissionFormRepository admissions;
    @Mock private RoleRepository roles;
    @Mock private UserRepository users;
    @Mock private AuthorizationSnapshotService authorizationSnapshots;
    @InjectMocks private HodModuleService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.HOD, 99L, 10L));
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        department.setName("Computer Science");
        StaffProfile hod = new StaffProfile();
        hod.setId(30L);
        hod.setCollege(college);
        hod.setDepartment(department);
        when(staff.findByUserId(99L)).thenReturn(Optional.of(hod));
        when(sections.findByDepartmentIdAndStatus(
                20L, com.jadhavr.erp.academic.enums.SectionStatus.ACTIVE))
                .thenReturn(List.of());
        when(staff.findTeachingByCollegeAndDepartment(
                10L, 20L, StaffStatus.ACTIVE, HodModuleServiceQueryTest.teachingTypes()))
                .thenReturn(List.of());
        when(timetables.findBySectionDepartmentIdAndSemesterOfferingStatusAndStatus(
                20L, SemesterOfferingStatus.ACTIVE, WeeklyTimetable.Status.ACTIVE))
                .thenReturn(List.of());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardUsesAggregateCountsInsteadOfLoadingSubjectsAttendanceAndEnrollments() {
        when(students.countByDepartmentIdAndStatus(20L, StudentStatus.ACTIVE)).thenReturn(120L);
        when(subjects.countByDepartmentIdAndStatus(20L, SubjectStatus.ACTIVE)).thenReturn(8L);
        when(timetableEntries.countDistinctTimetablesByDepartmentAndDay(
                20L, WeeklyTimetable.Status.ACTIVE, WeeklyTimetable.ReviewStatus.APPROVED,
                LocalDate.now().getDayOfWeek())).thenReturn(4L);
        when(attendance.countBySessionSectionDepartmentIdAndSessionStatus(
                20L, WeeklyAttendanceSession.Status.SUBMITTED)).thenReturn(100L);
        when(attendance.countBySessionSectionDepartmentIdAndSessionStatusAndStatusIn(
                20L, WeeklyAttendanceSession.Status.SUBMITTED,
                List.of(WeeklyAttendanceRecord.Status.PRESENT,
                        WeeklyAttendanceRecord.Status.LATE))).thenReturn(85L);
        when(enrollments.countBySectionDepartmentIdAndStatus(
                20L, AcademicStatus.ACTIVE)).thenReturn(115L);

        var summary = service.dashboard(null);

        assertEquals(120L, summary.totalStudents());
        assertEquals(8L, summary.totalSubjects());
        assertEquals(85.0, summary.averageAttendance());
        verify(subjects).countByDepartmentIdAndStatus(20L, SubjectStatus.ACTIVE);
        verify(enrollments).countBySectionDepartmentIdAndStatus(
                20L, AcademicStatus.ACTIVE);
    }

    private static java.util.Set<com.jadhavr.erp.staff.enums.StaffType> teachingTypes() {
        return java.util.EnumSet.of(
                com.jadhavr.erp.staff.enums.StaffType.HOD,
                com.jadhavr.erp.staff.enums.StaffType.TEACHER,
                com.jadhavr.erp.staff.enums.StaffType.CLASS_TEACHER,
                com.jadhavr.erp.staff.enums.StaffType.SUBJECT_TEACHER);
    }
}
