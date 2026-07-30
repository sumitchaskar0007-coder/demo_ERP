package com.jadhavr.erp.academic.service;

import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.SubjectStatus;
import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.AttendanceRecordRepository;
import com.jadhavr.erp.academic.repository.AttendanceSessionRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.StudentSectionEnrollmentRepository;
import com.jadhavr.erp.academic.repository.SubjectRepository;
import com.jadhavr.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.jadhavr.erp.academic.repository.TimetableEntryRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
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

    private void authenticate(RoleName role) {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(role, 99L, 10L));
    }
}
