package com.collegeerp.erp.academic.service;

import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.SubjectStatus;
import com.collegeerp.erp.academic.entity.AcademicClass;
import com.collegeerp.erp.academic.entity.CurriculumSemester;
import com.collegeerp.erp.academic.entity.Subject;
import com.collegeerp.erp.academic.dto.AcademicDtos.CreateSubject;
import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.academic.enums.SubjectType;
import com.collegeerp.erp.academic.repository.AcademicClassRepository;
import com.collegeerp.erp.academic.repository.AttendanceRecordRepository;
import com.collegeerp.erp.academic.repository.AttendanceSessionRepository;
import com.collegeerp.erp.academic.repository.SectionRepository;
import com.collegeerp.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.collegeerp.erp.academic.repository.SubjectRepository;
import com.collegeerp.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.collegeerp.erp.academic.repository.TimetableEntryRepository;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import com.collegeerp.erp.security.TestSecurityUsers;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.enums.StudentStatus;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.entity.RoleName;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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
class AcademicServiceImplQueryScopeTest {

    @Mock private AcademicClassRepository classes;
    @Mock private SectionRepository sections;
    @Mock private SubjectRepository subjects;
    @Mock private StudentSectionEnrollmentRepository enrollments;
    @Mock private SubjectTeacherAssignmentRepository assignments;
    @Mock private TimetableEntryRepository timetable;
    @Mock private AttendanceSessionRepository sessions;
    @Mock private AttendanceRecordRepository records;
    @Mock private CollegeRepository colleges;
    @Mock private DepartmentRepository departments;
    @Mock private StaffProfileRepository staff;
    @Mock private StudentProfileRepository students;
    @Mock private AcademicSessionResolver sessionResolver;
    @InjectMocks private AcademicServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void principalClassListIsBoundToCurrentCollege() {
        authenticate(RoleName.PRINCIPAL);
        College otherCollege = new College();
        otherCollege.setId(11L);
        Department department = new Department();
        department.setId(20L);
        AcademicClass crossTenantRow = new AcademicClass();
        crossTenantRow.setCollege(otherCollege);
        crossTenantRow.setDepartment(department);
        when(classes.findScoped(10L, Set.of(-1L), true, null))
                .thenReturn(List.of(crossTenantRow));

        assertEquals(List.of(), service.classes(null));

        verify(classes).findScoped(10L, Set.of(-1L), true, null);
    }

    @Test
    void hodSubjectAndAssignmentListsUseOnlyAssignedDepartments() {
        authenticate(RoleName.HOD);
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        StaffProfile profile = new StaffProfile();
        profile.setId(30L);
        profile.setCollege(college);
        profile.setDepartment(department);
        when(staff.findByUserId(99L)).thenReturn(Optional.of(profile));
        when(subjects.findScoped(10L, Set.of(20L), false,
                SubjectStatus.ACTIVE, null, null)).thenReturn(List.of());
        when(assignments.findScoped(10L, Set.of(20L), false,
                AcademicStatus.ACTIVE, null, null)).thenReturn(List.of());

        assertEquals(List.of(), service.subjects(null, null, null));
        assertEquals(List.of(), service.listAssignments(null, null));

        verify(subjects).findScoped(10L, Set.of(20L), false,
                SubjectStatus.ACTIVE, null, null);
        verify(assignments).findScoped(10L, Set.of(20L), false,
                AcademicStatus.ACTIVE, null, null);
    }

    @Test
    void eligibleStudentsUsesOneDepartmentAndAcademicYearQuery() {
        authenticate(RoleName.PRINCIPAL);
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        AcademicClass academicClass = new AcademicClass();
        academicClass.setCollege(college);
        academicClass.setDepartment(department);
        academicClass.setAcademicYear("2026-27");
        StudentProfile student = new StudentProfile();
        student.setId(40L);
        student.setAdmissionNumber("ADM-40");
        student.setFullName("Student Forty");
        student.setEmail("student40@example.test");
        student.setPhone("9000000040");
        when(classes.findById(30L)).thenReturn(Optional.of(academicClass));
        when(students.findEligibleForAcademicYear(
                20L, StudentStatus.ACTIVE, "2026-27", AcademicStatus.ACTIVE))
                .thenReturn(List.of(student));

        assertEquals(1, service.eligibleStudents(30L).size());

        verify(students).findEligibleForAcademicYear(
                20L, StudentStatus.ACTIVE, "2026-27", AcademicStatus.ACTIVE);
    }

    @Test
    void createsSubjectInTheSemesterSelectedForItsCourseYear() {
        authenticate(RoleName.PRINCIPAL);
        College college = new College();
        college.setId(10L);
        college.setName("College ERP College");
        Department department = new Department();
        department.setId(20L);
        department.setName("MCA");
        AcademicClass courseYear = new AcademicClass();
        courseYear.setId(30L);
        courseYear.setCollege(college);
        courseYear.setDepartment(department);
        courseYear.setAcademicYear("2026-27");
        courseYear.setYearName(CourseYearName.FIRST_YEAR);
        courseYear.setName("MCA First Year");
        courseYear.setCode("MCA-FY");
        courseYear.setStatus(AcademicStatus.ACTIVE);
        CurriculumSemester semester = org.mockito.Mockito.mock(CurriculumSemester.class);
        when(semester.getId()).thenReturn(81L);
        when(semester.getName()).thenReturn("Semester 2");
        when(semester.getSemesterNumber()).thenReturn(2);
        when(semester.getYearName()).thenReturn(CourseYearName.FIRST_YEAR);
        when(classes.findById(30L)).thenReturn(Optional.of(courseYear));
        when(sessionResolver.requireSemester(courseYear, 2)).thenReturn(semester);
        when(subjects.findByAcademicClassIdAndCurriculumSemesterIdAndCodeIgnoreCase(
                30L, 81L, "JAVA")).thenReturn(Optional.empty());
        when(subjects.save(org.mockito.ArgumentMatchers.any(Subject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createSubject(new CreateSubject(
                30L, 2, "2026/27", "Java", "java", null, 4, SubjectType.THEORY));

        assertEquals(2, response.get("semesterNumber"));
        verify(sessionResolver).requireSemester(courseYear, 2);
        verify(subjects).save(org.mockito.ArgumentMatchers.argThat(subject ->
                subject.getCurriculumSemester() == semester
                        && "2026-27".equals(subject.getAcademicYear())));
    }

    private void authenticate(RoleName role) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, 99L, 10L));
    }
}
