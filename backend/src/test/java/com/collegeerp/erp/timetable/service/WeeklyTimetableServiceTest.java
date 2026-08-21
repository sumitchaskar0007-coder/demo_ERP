package com.collegeerp.erp.timetable.service;

import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.entity.CurriculumSemester;
import com.collegeerp.erp.academic.entity.SemesterOffering;
import com.collegeerp.erp.academic.entity.StudentSectionEnrollment;
import com.collegeerp.erp.academic.entity.Subject;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.SubjectStatus;
import com.collegeerp.erp.academic.enums.SemesterOfferingStatus;
import com.collegeerp.erp.academic.repository.SectionRepository;
import com.collegeerp.erp.academic.repository.SubjectRepository;
import com.collegeerp.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.collegeerp.erp.academic.service.AcademicSessionResolver;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.audit.service.AuditLogService;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.notice.entity.NoticePriority;
import com.collegeerp.erp.notice.service.NoticeService;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.timetable.dto.WeeklyTimetableDtos.SaveEntryRequest;
import com.collegeerp.erp.timetable.dto.WeeklyTimetableDtos.PeriodItem;
import com.collegeerp.erp.timetable.dto.WeeklyTimetableDtos.UpdatePeriodsRequest;
import com.collegeerp.erp.timetable.entity.WeeklyPeriod;
import com.collegeerp.erp.timetable.entity.WeeklyTimetable;
import com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry;
import com.collegeerp.erp.timetable.repository.WeeklyPeriodRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableRepository;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyTimetableServiceTest {

    @Mock
    private WeeklyTimetableRepository tables;
    @Mock
    private WeeklyPeriodRepository periods;
    @Mock
    private WeeklyTimetableEntryRepository entries;
    @Mock
    private SectionRepository sections;
    @Mock
    private SubjectRepository subjects;
    @Mock
    private StaffProfileRepository staff;
    @Mock
    private SubjectTeacherAssignmentRepository subjectTeacherAssignments;
    @Mock
    private AuditLogService audit;
    @Mock
    private NoticeService notices;
    @Mock
    private AcademicSessionResolver sessionResolver;

    @InjectMocks
    private WeeklyTimetableService service;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_HOD"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveAllowsActiveHodWhenAssignedToSubject() {
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        com.collegeerp.erp.academic.entity.AcademicClass academicClass = new com.collegeerp.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);

        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);
        section.setAcademicYear("2025-26");
        StaffProfile editor = new StaffProfile();
        editor.setId(70L);
        editor.setCollege(college);
        editor.setDepartment(department);
        editor.setStaffType(StaffType.HOD);
        editor.setStatus(StaffStatus.ACTIVE);
        section.setClassTeacher(editor);

        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setCollege(college);
        timetable.setSection(section);
        SemesterOffering currentOffering = activeOffering();
        timetable.setSemesterOffering(currentOffering);
        timetable.setStatus(WeeklyTimetable.Status.DRAFT);
        setId(timetable, 1L);

        WeeklyPeriod period = new WeeklyPeriod();
        period.setTimetable(timetable);
        setId(period, 2L);
        period.setKind(WeeklyPeriod.Kind.TEACHING);
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(9, 0));

        Subject subject = new Subject();
        subject.setAcademicClass(academicClass);
        subject.setCurriculumSemester(currentOffering.getCurriculumSemester());
        subject.setStatus(SubjectStatus.ACTIVE);
        setId(subject, 50L);

        StaffProfile teacher = new StaffProfile();
        teacher.setId(60L);
        teacher.setStatus(StaffStatus.ACTIVE);
        teacher.setDepartment(department);
        teacher.setStaffType(StaffType.HOD);

        when(tables.findById(1L)).thenReturn(Optional.of(timetable));
        when(periods.findById(2L)).thenReturn(Optional.of(period));
        when(subjects.findById(50L)).thenReturn(Optional.of(subject));
        when(staff.findById(60L)).thenReturn(Optional.of(teacher));
        when(staff.findByUserId(99L)).thenReturn(Optional.of(editor));
        when(subjectTeacherAssignments.existsBySubjectIdAndTeacherIdAndStatus(
                50L, 60L, AcademicStatus.ACTIVE)).thenReturn(true);
        when(entries.save(org.mockito.ArgumentMatchers.any(WeeklyTimetableEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SaveEntryRequest request = new SaveEntryRequest(50L, 60L, "A101", "THEORY", "");

        var result = service.save(1L, "MONDAY", 2L, request);

        assertEquals(60L, result.teacherId());
        assertEquals("THEORY", result.lectureType());
    }

    @Test
    void submitForReviewCreatesPrincipalNotice() {
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        department.setName("BCA");
        com.collegeerp.erp.academic.entity.AcademicClass academicClass =
                new com.collegeerp.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);
        academicClass.setName("BCA First Year");

        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);
        section.setName("Division A");
        section.setAcademicYear("2026-2027");

        StaffProfile editor = new StaffProfile();
        editor.setId(70L);
        editor.setCollege(college);
        editor.setDepartment(department);
        editor.setStaffType(StaffType.HOD);
        editor.setStatus(StaffStatus.ACTIVE);

        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setCollege(college);
        timetable.setSection(section);
        timetable.setSemesterOffering(activeOffering());
        timetable.setStatus(WeeklyTimetable.Status.DRAFT);
        setId(timetable, 1L);

        when(tables.findById(1L)).thenReturn(Optional.of(timetable));
        when(staff.findByUserId(99L)).thenReturn(Optional.of(editor));
        when(entries.findByTimetableId(1L))
                .thenReturn(List.of(new com.collegeerp.erp.timetable.entity.WeeklyTimetableEntry()))
                .thenReturn(Collections.emptyList());
        when(periods.findByTimetableIdOrderByPosition(1L)).thenReturn(Collections.emptyList());

        service.submitForReview(1L);

        assertEquals(WeeklyTimetable.ReviewStatus.SUBMITTED, timetable.getReviewStatus());
        verify(notices).createWorkflowNotice(
                eq("Timetable awaiting approval"),
                contains("BCA First Year · Division A"),
                eq(NoticePriority.NORMAL),
                eq(Set.of(RoleName.PRINCIPAL)),
                same(college),
                eq("/timetable?sectionId=40"));
    }

    @Test
    void approvingUpdateArchivesPreviousLiveTimetable() {
        setSecurityRole(RoleName.PRINCIPAL);
        College college = new College();
        college.setId(10L);
        college.setName("College ERP College");
        Department department = new Department();
        department.setId(20L);
        department.setName("BCA");
        com.collegeerp.erp.academic.entity.AcademicClass academicClass =
                new com.collegeerp.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);
        academicClass.setName("BCA First Year");
        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);
        section.setName("Division A");
        section.setAcademicYear("2026-2027");

        WeeklyTimetable live = new WeeklyTimetable();
        live.setCollege(college);
        live.setSection(section);
        SemesterOffering offering = activeOffering();
        live.setSemesterOffering(offering);
        live.setStatus(WeeklyTimetable.Status.ACTIVE);
        live.setReviewStatus(WeeklyTimetable.ReviewStatus.APPROVED);
        setId(live, 1L);

        WeeklyTimetable revision = new WeeklyTimetable();
        revision.setCollege(college);
        revision.setSection(section);
        revision.setSemesterOffering(offering);
        revision.setStatus(WeeklyTimetable.Status.DRAFT);
        revision.setReviewStatus(WeeklyTimetable.ReviewStatus.SUBMITTED);
        setId(revision, 2L);

        when(tables.findById(2L)).thenReturn(Optional.of(revision));
        when(tables.findFirstBySectionIdAndStatusAndSemesterOfferingStatusOrderByIdDesc(
                40L, WeeklyTimetable.Status.ACTIVE, SemesterOfferingStatus.ACTIVE)).thenReturn(Optional.of(live));
        when(entries.findByTimetableId(2L)).thenReturn(Collections.emptyList());
        when(periods.findByTimetableIdOrderByPosition(2L)).thenReturn(Collections.emptyList());

        service.review(2L, new com.collegeerp.erp.timetable.dto.WeeklyTimetableDtos.ReviewRequest(
                "APPROVE", null));

        assertEquals(WeeklyTimetable.Status.ARCHIVED, live.getStatus());
        assertEquals(WeeklyTimetable.Status.ACTIVE, revision.getStatus());
        assertEquals(WeeklyTimetable.ReviewStatus.APPROVED, revision.getReviewStatus());
        verify(tables).saveAndFlush(live);
        verify(tables).save(revision);
    }

    @Test
    void studentCannotOpenTimetableBeforePrincipalApproval() {
        setSecurityRole(RoleName.STUDENT);
        Section section = new Section();
        section.setId(40L);
        StudentSectionEnrollment enrollment = new StudentSectionEnrollment();
        enrollment.setSection(section);
        enrollment.setStatus(AcademicStatus.ACTIVE);
        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setSection(section);
        timetable.setReviewStatus(WeeklyTimetable.ReviewStatus.SUBMITTED);
        when(tables.findFirstBySectionIdAndStatusAndSemesterOfferingStatusOrderByIdDesc(
                40L, WeeklyTimetable.Status.ACTIVE, SemesterOfferingStatus.ACTIVE)).thenReturn(Optional.of(timetable));

        assertThrows(ResourceNotFoundException.class, () -> service.studentTimetable(enrollment));
    }

    @Test
    void changingPeriodTimeClearsItsExistingLectureAssignments() {
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        com.collegeerp.erp.academic.entity.AcademicClass academicClass =
                new com.collegeerp.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);

        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);

        StaffProfile editor = new StaffProfile();
        editor.setId(70L);
        editor.setCollege(college);
        editor.setDepartment(department);
        editor.setStaffType(StaffType.HOD);
        editor.setStatus(StaffStatus.ACTIVE);

        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setCollege(college);
        timetable.setSection(section);
        timetable.setSemesterOffering(activeOffering());
        timetable.setStatus(WeeklyTimetable.Status.DRAFT);
        setId(timetable, 1L);

        WeeklyPeriod period = new WeeklyPeriod();
        period.setTimetable(timetable);
        period.setPosition(1);
        period.setLabel("Period 1");
        period.setStartTime(LocalTime.of(13, 0));
        period.setEndTime(LocalTime.of(14, 0));
        period.setKind(WeeklyPeriod.Kind.TEACHING);
        setId(period, 2L);

        WeeklyTimetableEntry assignment = new WeeklyTimetableEntry();
        assignment.setTimetable(timetable);
        assignment.setPeriod(period);

        when(tables.findById(1L)).thenReturn(Optional.of(timetable));
        when(staff.findByUserId(99L)).thenReturn(Optional.of(editor));
        when(periods.findByTimetableIdOrderByPosition(1L)).thenReturn(List.of(period));
        when(entries.findByPeriodId(2L)).thenReturn(List.of(assignment));
        when(entries.findByTimetableId(1L)).thenReturn(Collections.emptyList());

        service.updatePeriods(1L, new UpdatePeriodsRequest(List.of(
                new PeriodItem(2L, "Period 1", LocalTime.of(14, 0),
                        LocalTime.of(15, 0), WeeklyPeriod.Kind.TEACHING))));

        verify(entries).deleteAll(List.of(assignment));
        assertEquals(LocalTime.of(14, 0), period.getStartTime());
        assertEquals(LocalTime.of(15, 0), period.getEndTime());
    }

    @Test
    void divisionsUsesPrincipalCollegeScopeInsteadOfLoadingEveryDivision() {
        setSecurityRole(RoleName.PRINCIPAL);
        when(sections.findByCollegeIdAndStatus(10L,
                com.collegeerp.erp.academic.enums.SectionStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        assertEquals(List.of(), service.divisions());

        verify(sections).findByCollegeIdAndStatus(10L,
                com.collegeerp.erp.academic.enums.SectionStatus.ACTIVE);
    }

    @Test
    void timetableOffersOnlySubjectsFromItsActiveSemester() {
        College college = new College();
        college.setId(10L);
        college.setName("College ERP College");
        Department department = new Department();
        department.setId(20L);
        department.setName("MCA");
        com.collegeerp.erp.academic.entity.AcademicClass academicClass =
                new com.collegeerp.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);
        academicClass.setName("MCA First Year");
        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);
        section.setName("Division A");
        section.setAcademicYear("2026-2027");
        StaffProfile editor = new StaffProfile();
        editor.setId(70L);
        editor.setCollege(college);
        editor.setDepartment(department);
        editor.setStaffType(StaffType.HOD);
        editor.setStatus(StaffStatus.ACTIVE);
        CurriculumSemester semester = new CurriculumSemester();
        setId(semester, 81L);
        semester.setSemesterNumber(1);
        semester.setName("Semester 1");
        SemesterOffering offering = new SemesterOffering();
        setId(offering, 82L);
        offering.setCurriculumSemester(semester);
        offering.setStatus(SemesterOfferingStatus.ACTIVE);
        WeeklyTimetable timetable = new WeeklyTimetable();
        setId(timetable, 1L);
        timetable.setCollege(college);
        timetable.setSection(section);
        timetable.setSemesterOffering(offering);
        timetable.setStatus(WeeklyTimetable.Status.DRAFT);
        when(sections.findById(40L)).thenReturn(Optional.of(section));
        when(staff.findByUserId(99L)).thenReturn(Optional.of(editor));
        when(tables.findFirstBySectionIdAndStatusAndSemesterOfferingStatusOrderByIdDesc(
                40L, WeeklyTimetable.Status.DRAFT, SemesterOfferingStatus.ACTIVE))
                .thenReturn(Optional.of(timetable));
        when(tables.findFirstBySectionIdAndStatusAndSemesterOfferingStatusOrderByIdDesc(
                40L, WeeklyTimetable.Status.ACTIVE, SemesterOfferingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subjects.findByAcademicClassIdAndCurriculumSemesterIdAndStatusOrderByCodeAsc(
                30L, 81L, SubjectStatus.ACTIVE)).thenReturn(List.of());
        when(staff.findTeachingByCollegeAndDepartment(eq(10L), eq(20L), eq(StaffStatus.ACTIVE),
                eq(Set.of(StaffType.HOD, StaffType.TEACHER,
                        StaffType.CLASS_TEACHER, StaffType.SUBJECT_TEACHER))))
                .thenReturn(List.of());
        when(entries.findByTimetableId(1L)).thenReturn(List.of());
        when(periods.findByTimetableIdOrderByPosition(1L)).thenReturn(List.of());

        var response = service.getOrCreate(40L);

        assertEquals(1, response.semesterNumber());
        assertEquals("Semester 1", response.semesterName());
        verify(subjects).findByAcademicClassIdAndCurriculumSemesterIdAndStatusOrderByCodeAsc(
                30L, 81L, SubjectStatus.ACTIVE);
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private SemesterOffering activeOffering() {
        CurriculumSemester semester = new CurriculumSemester();
        setId(semester, 81L);
        semester.setSemesterNumber(1);
        semester.setName("Semester 1");
        SemesterOffering offering = new SemesterOffering();
        setId(offering, 82L);
        offering.setCurriculumSemester(semester);
        offering.setStatus(SemesterOfferingStatus.ACTIVE);
        return offering;
    }

    private CustomUserDetails userDetails() {
        return userDetails(RoleName.HOD);
    }

    private void setSecurityRole(RoleName roleName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails(roleName), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName.name()))));
    }

    private CustomUserDetails userDetails(RoleName roleName) {
        User user = new User();
        user.setId(99L);
        user.setEmail("admin@example.com");
        user.setFullName("Admin");
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("secret");
        Role role = new Role();
        College college = new College();
        college.setId(10L);
        user.setCollege(college);
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return new CustomUserDetails(user);
    }
}
