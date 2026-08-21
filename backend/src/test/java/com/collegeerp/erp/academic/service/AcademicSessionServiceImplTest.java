package com.collegeerp.erp.academic.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.collegeerp.erp.academic.dto.AcademicSessionDtos.ActivateAcademicTermRequest;
import com.collegeerp.erp.academic.dto.AcademicSessionDtos.ExecuteRolloverRequest;
import com.collegeerp.erp.academic.entity.AcademicModels.AcademicTerm;
import com.collegeerp.erp.academic.entity.AcademicModels.AcademicYear;
import com.collegeerp.erp.academic.entity.CurriculumSemester;
import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.entity.SemesterOffering;
import com.collegeerp.erp.academic.entity.SemesterRolloverJob;
import com.collegeerp.erp.academic.entity.StudentSectionEnrollment;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.AcademicTermStatus;
import com.collegeerp.erp.academic.enums.AcademicTermType;
import com.collegeerp.erp.academic.enums.AcademicYearStatus;
import com.collegeerp.erp.academic.repository.*;
import com.collegeerp.erp.audit.enums.AuditAction;
import com.collegeerp.erp.audit.enums.AuditModule;
import com.collegeerp.erp.audit.service.AuditLogService;
import com.collegeerp.erp.auth.security.AuthorizationSnapshot;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.user.entity.UserStatus;
import com.collegeerp.erp.user.repository.UserRepository;
import com.collegeerp.erp.timetable.repository.WeeklyTimetableRepository;
import com.collegeerp.erp.timetable.entity.WeeklyTimetable;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AcademicSessionServiceImplTest {
    @Mock AcademicYearRepository years;
    @Mock AcademicTermRepository terms;
    @Mock CurriculumSemesterRepository semesters;
    @Mock SemesterOfferingRepository offerings;
    @Mock StudentSectionEnrollmentRepository enrollments;
    @Mock SectionRepository sections;
    @Mock DepartmentRepository departments;
    @Mock CollegeRepository colleges;
    @Mock UserRepository users;
    @Mock SemesterRolloverJobRepository jobs;
    @Mock SemesterRolloverItemRepository items;
    @Mock AuditLogService audit;
    @Mock WeeklyTimetableRepository timetables;

    private AcademicSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-17T10:00:00Z"), ZoneId.of("Asia/Kolkata"));
        service = new AcademicSessionServiceImpl(years, terms, semesters, offerings, enrollments,
                sections, departments, colleges, users, jobs, items, audit, timetables, clock);
        CustomUserDetails principal = new CustomUserDetails(new AuthorizationSnapshot(
                10L, 1L, "principal@example.test", UserStatus.ACTIVE, null, 1L,
                List.of("ROLE_PRINCIPAL")));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsFutureSemesterActivationWithoutAnOverride() {
        AcademicTerm even = term(2L, AcademicTermType.EVEN, LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 6, 30), AcademicTermStatus.PLANNED);
        when(terms.findByIdAndCollegeId(2L, 1L)).thenReturn(Optional.of(even));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.activateTerm(2L, new ActivateAcademicTermRequest(false, null)));
        verify(audit, never()).log(eq(AuditModule.ACADEMIC), eq(AuditAction.ACTIVATE),
                eq("AcademicTerm"), eq(2L), contains("Activated"));
    }

    @Test
    void permitsAndAuditsAnExplicitFutureDateOverride() {
        AcademicTerm even = term(2L, AcademicTermType.EVEN, LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 6, 30), AcademicTermStatus.PLANNED);
        when(terms.findByIdAndCollegeId(2L, 1L)).thenReturn(Optional.of(even));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.empty());
        when(offerings.findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(2L))
                .thenReturn(List.of());

        service.activateTerm(2L,
                new ActivateAcademicTermRequest(true, "Emergency calendar correction"));

        verify(even).setStatus(AcademicTermStatus.ACTIVE);
        verify(audit).log(eq(AuditModule.ACADEMIC), eq(AuditAction.ACTIVATE),
                eq("AcademicTerm"), eq(2L), contains("Emergency calendar correction"));
    }

    @Test
    void directActivationCannotBypassRolloverForActiveStudents() {
        AcademicTerm odd = term(1L, AcademicTermType.ODD, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31), AcademicTermStatus.ACTIVE);
        AcademicTerm even = term(2L, AcademicTermType.EVEN, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 31), AcademicTermStatus.PLANNED);
        when(terms.findByIdAndCollegeId(2L, 1L)).thenReturn(Optional.of(even));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.of(odd));
        when(enrollments.existsBySemesterOfferingAcademicTermIdAndStatus(1L, AcademicStatus.ACTIVE))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.activateTerm(2L, new ActivateAcademicTermRequest(false, null)));
    }

    @Test
    void rolloverCannotRunBeforeTargetSemesterStarts() {
        AcademicTerm odd = term(1L, AcademicTermType.ODD, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 12, 31), AcademicTermStatus.ACTIVE);
        AcademicTerm even = term(2L, AcademicTermType.EVEN, LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 6, 30), AcademicTermStatus.PLANNED);
        when(terms.findByIdAndCollegeId(1L, 1L)).thenReturn(Optional.of(odd));
        when(terms.findByIdAndCollegeId(2L, 1L)).thenReturn(Optional.of(even));

        ExecuteRolloverRequest request = new ExecuteRolloverRequest(
                1L, 2L, Map.of(), Set.of(), "PROMOTE");
        assertThrows(BadRequestException.class, () -> service.execute(request));
        verify(jobs, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rolloverFlushesCompletedEnrollmentBeforeReusingItsRollNumber() {
        AcademicTerm odd = term(1L, AcademicTermType.ODD, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 16), AcademicTermStatus.ACTIVE);
        AcademicTerm even = term(2L, AcademicTermType.EVEN, LocalDate.of(2026, 8, 17),
                LocalDate.of(2027, 6, 30), AcademicTermStatus.PLANNED);
        AcademicYear year = odd.getAcademicYear();
        when(even.getAcademicYear()).thenReturn(year);
        when(terms.findByIdAndCollegeId(1L, 1L)).thenReturn(Optional.of(odd));
        when(terms.findByIdAndCollegeId(2L, 1L)).thenReturn(Optional.of(even));
        when(jobs.findByCollegeIdAndSourceTermIdAndTargetTermId(1L, 1L, 2L)).thenReturn(Optional.empty());
        when(users.findById(10L)).thenReturn(Optional.empty());

        College college = org.mockito.Mockito.mock(College.class);
        Department department = org.mockito.Mockito.mock(Department.class);
        StudentProfile student = org.mockito.Mockito.mock(StudentProfile.class);
        Section section = org.mockito.Mockito.mock(Section.class);
        SemesterOffering sourceOffering = org.mockito.Mockito.mock(SemesterOffering.class);
        SemesterOffering targetOffering = org.mockito.Mockito.mock(SemesterOffering.class);
        CurriculumSemester first = org.mockito.Mockito.mock(CurriculumSemester.class);
        CurriculumSemester second = org.mockito.Mockito.mock(CurriculumSemester.class);
        StudentSectionEnrollment current = org.mockito.Mockito.mock(StudentSectionEnrollment.class);
        when(odd.getCollege()).thenReturn(college);
        when(current.getStudent()).thenReturn(student);
        when(current.getSection()).thenReturn(section);
        when(current.getSemesterOffering()).thenReturn(sourceOffering);
        when(current.getRollNumber()).thenReturn("1");
        when(student.getId()).thenReturn(50L);
        when(sourceOffering.getCurriculumSemester()).thenReturn(first);
        when(first.getDepartment()).thenReturn(department);
        when(first.getSemesterNumber()).thenReturn(1);
        when(department.getId()).thenReturn(5L);
        when(second.isActive()).thenReturn(true);
        when(second.getId()).thenReturn(22L);
        when(second.getName()).thenReturn("Semester 2");
        when(semesters.findByDepartmentIdAndSemesterNumber(5L, 2)).thenReturn(Optional.of(second));
        when(offerings.findByAcademicTermIdAndCurriculumSemesterId(2L, 22L)).thenReturn(Optional.of(targetOffering));
        when(targetOffering.getId()).thenReturn(32L);
        when(section.getId()).thenReturn(7L);
        when(section.getCapacity()).thenReturn(60);
        when(enrollments.countBySectionIdAndStatus(7L, AcademicStatus.ACTIVE)).thenReturn(1L);
        when(enrollments.existsByStudentIdAndSemesterOfferingId(50L, 32L)).thenReturn(false);
        when(enrollments.findBySemesterOfferingAcademicTermIdAndStatus(1L, AcademicStatus.ACTIVE)).thenReturn(List.of(current));
        when(enrollments.save(any(StudentSectionEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobs.save(any(SemesterRolloverJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(terms.findByCollegeIdAndStatus(1L, AcademicTermStatus.ACTIVE)).thenReturn(Optional.of(odd));
        when(offerings.findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(1L)).thenReturn(List.of());
        when(offerings.findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(2L)).thenReturn(List.of());
        WeeklyTimetable oldTimetable = new WeeklyTimetable();
        oldTimetable.setStatus(WeeklyTimetable.Status.ACTIVE);
        when(timetables.findBySemesterOfferingAcademicTermIdAndStatusNot(
                1L, WeeklyTimetable.Status.ARCHIVED)).thenReturn(List.of(oldTimetable));

        service.execute(new ExecuteRolloverRequest(1L, 2L, Map.of(), Set.of(), "PROMOTE"));

        InOrder order = org.mockito.Mockito.inOrder(enrollments);
        order.verify(enrollments).flush();
        order.verify(enrollments).save(any(StudentSectionEnrollment.class));
        verify(current).setStatus(AcademicStatus.INACTIVE);
        org.junit.jupiter.api.Assertions.assertEquals(
                WeeklyTimetable.Status.ARCHIVED, oldTimetable.getStatus());
    }

    private AcademicTerm term(Long id, AcademicTermType type, LocalDate start,
            LocalDate end, AcademicTermStatus status) {
        AcademicYear year = org.mockito.Mockito.mock(AcademicYear.class);
        org.mockito.Mockito.lenient().when(year.getId()).thenReturn(100L);
        org.mockito.Mockito.lenient().when(year.getStatus()).thenReturn(AcademicYearStatus.ACTIVE);
        org.mockito.Mockito.lenient().when(year.getName()).thenReturn("2026-2027");
        AcademicTerm term = org.mockito.Mockito.mock(AcademicTerm.class);
        org.mockito.Mockito.lenient().when(term.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(term.getAcademicYear()).thenReturn(year);
        org.mockito.Mockito.lenient().when(term.getName()).thenReturn(type == AcademicTermType.ODD ? "Odd Semester" : "Even Semester");
        org.mockito.Mockito.lenient().when(term.getTermType()).thenReturn(type);
        org.mockito.Mockito.lenient().when(term.getStartDate()).thenReturn(start);
        org.mockito.Mockito.lenient().when(term.getEndDate()).thenReturn(end);
        org.mockito.Mockito.lenient().when(term.getStatus()).thenReturn(status);
        return term;
    }
}
