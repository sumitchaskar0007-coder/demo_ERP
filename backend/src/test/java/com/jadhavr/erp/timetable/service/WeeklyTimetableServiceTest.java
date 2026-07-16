package com.jadhavr.erp.timetable.service;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.academic.entity.Subject;
import com.jadhavr.erp.academic.enums.AcademicStatus;
import com.jadhavr.erp.academic.enums.SubjectStatus;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.SubjectRepository;
import com.jadhavr.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.timetable.dto.WeeklyTimetableDtos.SaveEntryRequest;
import com.jadhavr.erp.timetable.entity.WeeklyPeriod;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.timetable.repository.WeeklyPeriodRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableEntryRepository;
import com.jadhavr.erp.timetable.repository.WeeklyTimetableRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @InjectMocks
    private WeeklyTimetableService service;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveRejectsTeacherNotAssignedToSubject() {
        College college = new College();
        college.setId(10L);
        Department department = new Department();
        department.setId(20L);
        com.jadhavr.erp.academic.entity.AcademicClass academicClass = new com.jadhavr.erp.academic.entity.AcademicClass();
        academicClass.setId(30L);

        Section section = new Section();
        section.setId(40L);
        section.setCollege(college);
        section.setDepartment(department);
        section.setAcademicClass(academicClass);
        section.setAcademicYear("2025-26");

        WeeklyTimetable timetable = new WeeklyTimetable();
        timetable.setCollege(college);
        timetable.setSection(section);
        setId(timetable, 1L);

        WeeklyPeriod period = new WeeklyPeriod();
        period.setTimetable(timetable);
        setId(period, 2L);
        period.setKind(WeeklyPeriod.Kind.TEACHING);
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(9, 0));

        Subject subject = new Subject();
        subject.setAcademicClass(academicClass);
        subject.setStatus(SubjectStatus.ACTIVE);
        setId(subject, 50L);

        StaffProfile teacher = new StaffProfile();
        teacher.setId(60L);
        teacher.setStatus(StaffStatus.ACTIVE);
        teacher.setDepartment(department);
        teacher.setStaffType(StaffType.SUBJECT_TEACHER);

        when(tables.findById(1L)).thenReturn(Optional.of(timetable));
        when(periods.findById(2L)).thenReturn(Optional.of(period));
        when(subjects.findById(50L)).thenReturn(Optional.of(subject));
        when(staff.findById(60L)).thenReturn(Optional.of(teacher));
        when(subjectTeacherAssignments.existsBySubjectIdAndTeacherIdAndStatus(50L, 60L, AcademicStatus.ACTIVE)).thenReturn(false);

        SaveEntryRequest request = new SaveEntryRequest(50L, 60L, "A101", "THEORY", "");

        assertThrows(BadRequestException.class, () -> service.save(1L, "MONDAY", 2L, request));
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

    private CustomUserDetails userDetails() {
        User user = new User();
        user.setId(99L);
        user.setEmail("admin@example.com");
        user.setFullName("Admin");
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("secret");
        Role role = new Role();
        role.setName(RoleName.SUPER_ADMIN);
        user.setRoles(Set.of(role));
        return new CustomUserDetails(user);
    }
}
