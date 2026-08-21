package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.SubjectStatus;
import com.collegeerp.erp.academic.enums.SemesterOfferingStatus;
import com.collegeerp.erp.academic.repository.ClassTeacherAssignmentHistoryRepository;
import com.collegeerp.erp.academic.repository.SectionRepository;
import com.collegeerp.erp.academic.repository.StudentDivisionTransferRepository;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.academic.repository.SubjectRepository;
import com.collegeerp.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.attendance.entity.WeeklyAttendanceRecord;
import com.collegeerp.erp.attendance.entity.WeeklyAttendanceSession;
import com.collegeerp.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.collegeerp.erp.audit.service.AuditLogService;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.enums.StudentStatus;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.teacher.service.TeacherNotificationService;
import com.collegeerp.erp.timetable.entity.WeeklyTimetable;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableRepository;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.repository.RoleRepository;
import com.collegeerp.erp.user.repository.UserRepository;
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
                20L, com.collegeerp.erp.academic.enums.SectionStatus.ACTIVE))
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

    private static java.util.Set<com.collegeerp.erp.staff.enums.StaffType> teachingTypes() {
        return java.util.EnumSet.of(
                com.collegeerp.erp.staff.enums.StaffType.HOD,
                com.collegeerp.erp.staff.enums.StaffType.TEACHER,
                com.collegeerp.erp.staff.enums.StaffType.CLASS_TEACHER,
                com.collegeerp.erp.staff.enums.StaffType.SUBJECT_TEACHER);
    }
}
