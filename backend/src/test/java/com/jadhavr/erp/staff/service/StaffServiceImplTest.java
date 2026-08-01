package com.jadhavr.erp.staff.service;

import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.auth.security.AuthorizationSnapshotService;
import com.jadhavr.erp.auth.repository.RefreshTokenRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.email.service.EmailNotificationService;
import com.jadhavr.erp.staff.dto.CreateStudentSectionStaffRequest;
import com.jadhavr.erp.staff.dto.CreateStaffRequest;
import com.jadhavr.erp.staff.dto.StaffResponse;
import com.jadhavr.erp.staff.dto.UpdateStaffAssignmentRequest;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceRecordRepository;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.staff.mapper.StaffMapper;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {
    @Mock private StaffProfileRepository staffProfiles;
    @Mock private UserRepository users;
    @Mock private RoleRepository roles;
    @Mock private CollegeRepository colleges;
    @Mock private DepartmentRepository departments;
    @Mock private SectionRepository sections;
    @Mock private SubjectTeacherAssignmentRepository subjectAssignments;
    @Mock private WeeklyAttendanceSessionRepository attendanceSessions;
    @Mock private WeeklyAttendanceRecordRepository attendanceRecords;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private AuthorizationSnapshotService authorizationSnapshots;
    @Mock private EmailNotificationService emailNotifications;

    private BCryptPasswordEncoder passwordEncoder;
    private StaffServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new StaffServiceImpl(
                staffProfiles, users, roles, colleges, passwordEncoder, new StaffMapper(),
                refreshTokens, authorizationSnapshots);
        service.setDepartments(departments);
        service.setEmailNotifications(emailNotifications);
        service.setStaffDetailRepositories(
                sections, subjectAssignments, attendanceSessions, attendanceRecords);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStudentSectionStaffSucceedsByPrincipal() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        College college = college(1L, CollegeStatus.ACTIVE);
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(users.existsByEmail("section@example.com")).thenReturn(false);
        when(roles.findByName(RoleName.STUDENT_SECTION)).thenReturn(Optional.of(role(RoleName.STUDENT_SECTION)));
        when(staffProfiles.existsByEmployeeCode(anyString())).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(staffProfiles.save(any(StaffProfile.class))).thenAnswer(invocation -> {
            StaffProfile profile = invocation.getArgument(0);
            profile.setId(20L);
            return profile;
        });

        var result = service.createStudentSectionStaff(request(1L));

        assertEquals(20L, result.id());
        assertEquals(StaffType.STUDENT_SECTION, result.staffType());
        assertEquals(StaffStatus.ACTIVE, result.status());
        assertTrue(result.employeeCode().startsWith("EMP-ABC001-"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals(RoleName.STUDENT_SECTION, userCaptor.getValue().getRoles().iterator().next().getName());
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotifications).queueUserCreatedEmail(
                org.mockito.ArgumentMatchers.eq(userCaptor.getValue()), passwordCaptor.capture());
        assertTrue(passwordEncoder.matches(
                passwordCaptor.getValue(), userCaptor.getValue().getPasswordHash()));
        assertNotEquals("9876543210", passwordCaptor.getValue());
        assertTrue(userCaptor.getValue().isMustChangePassword());
    }

    @Test
    void principalCannotCreateStaffForAnotherCollege() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);

        assertThrows(AccessDeniedException.class,
                () -> service.createStudentSectionStaff(request(99L)));
    }

    @Test
    void createStaffFailsIfCollegeInactive() {
        authenticate(1L, null, RoleName.SUPER_ADMIN);
        when(colleges.findById(1L)).thenReturn(Optional.of(college(1L, CollegeStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.createStudentSectionStaff(request(1L)));
    }

    @Test
    void createStaffFailsIfEmailAlreadyExists() {
        authenticate(1L, null, RoleName.SUPER_ADMIN);
        when(colleges.findById(1L)).thenReturn(Optional.of(college(1L, CollegeStatus.ACTIVE)));
        when(users.existsByEmail("section@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createStudentSectionStaff(request(1L)));
    }

    @Test
    void deactivateStaffAlsoDeactivatesUser() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        StaffProfile profile = staffProfile();
        when(staffProfiles.findById(20L)).thenReturn(Optional.of(profile));
        when(staffProfiles.save(profile)).thenReturn(profile);

        var result = service.deactivateStaff(20L);

        assertEquals(StaffStatus.INACTIVE, result.status());
        assertEquals(UserStatus.INACTIVE, profile.getUser().getStatus());
    }

    @Test
    void activateStaffAlsoActivatesUser() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        StaffProfile profile = staffProfile();
        profile.setStatus(StaffStatus.INACTIVE);
        profile.getUser().setStatus(UserStatus.INACTIVE);
        when(staffProfiles.findById(20L)).thenReturn(Optional.of(profile));
        when(staffProfiles.save(profile)).thenReturn(profile);

        var result = service.activateStaff(20L);

        assertEquals(StaffStatus.ACTIVE, result.status());
        assertEquals(UserStatus.ACTIVE, profile.getUser().getStatus());
    }

    @Test
    void principalCanUpdateStaffRoleAndClearDepartmentsForOperationalStaff() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        StaffProfile profile = staffProfile();
        profile.setDepartment(department(5L, 1L));
        profile.setDepartments(Set.of(profile.getDepartment()));
        when(staffProfiles.findById(20L)).thenReturn(Optional.of(profile));
        when(sections.findByClassTeacherIdAndStatus(any(), any())).thenReturn(List.of());
        when(subjectAssignments.findByTeacherIdAndStatus(any(), any())).thenReturn(List.of());
        when(roles.findByName(RoleName.FEE_SECTION))
                .thenReturn(Optional.of(role(RoleName.FEE_SECTION)));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(staffProfiles.save(profile)).thenReturn(profile);

        StaffResponse result = service.updateStaffAssignment(
                20L,
                new UpdateStaffAssignmentRequest(Set.of(StaffType.FEE_SECTION), Set.of()));

        assertEquals(StaffType.FEE_SECTION, result.staffType());
        assertTrue(result.departmentIds().isEmpty());
        assertEquals(Set.of("FEE_SECTION"), result.roles());
        assertEquals(1L, profile.getUser().getSessionVersion());
    }

    @Test
    void principalCannotUpdateStaffFromAnotherCollege() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        StaffProfile profile = staffProfile();
        profile.setCollege(college(99L, CollegeStatus.ACTIVE));
        when(staffProfiles.findById(20L)).thenReturn(Optional.of(profile));

        assertThrows(
                AccessDeniedException.class,
                () -> service.updateStaffAssignment(
                        20L,
                        new UpdateStaffAssignmentRequest(
                                Set.of(StaffType.GENERAL_STAFF), Set.of())));
    }

    @Test
    void unifiedFormUsesRandomEmailedTemporaryPasswordAndRequiresChange() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        Department department = department(5L, 1L);
        when(departments.findById(5L)).thenReturn(Optional.of(department));
        stubUnifiedCreation(RoleName.SUBJECT_TEACHER);

        var result = service.createStaff(unified(StaffType.TEACHER, 5L, "teacher@example.com"));

        assertEquals(StaffType.TEACHER, result.staffType());
        assertEquals(5L, result.departmentId());
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(users).save(user.capture());
        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(emailNotifications).queueUserCreatedEmail(
                org.mockito.ArgumentMatchers.eq(user.getValue()), password.capture());
        assertTrue(passwordEncoder.matches(password.getValue(), user.getValue().getPasswordHash()));
        assertNotEquals("9876543210", password.getValue());
        assertTrue(user.getValue().isMustChangePassword());
    }

    @Test
    void unifiedFormRejectsDirectClassTeacherCreation() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        CreateStaffRequest request = new CreateStaffRequest("Multi Teacher", "multi@example.com",
                "9876543210", 5L, StaffType.SUBJECT_TEACHER,
                Set.of(5L, 6L), Set.of(StaffType.SUBJECT_TEACHER, StaffType.CLASS_TEACHER),
                LocalDate.of(2026, 7, 10));

        BadRequestException error = assertThrows(
                BadRequestException.class, () -> service.createStaff(request));
        assertTrue(error.getMessage().contains("Create the staff member as Teacher"));
        verifyNoInteractions(departments);
    }

    @Test
    void unifiedFormCreatesStudentSectionWithoutDepartment() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        stubUnifiedCreation(RoleName.STUDENT_SECTION);
        assertEquals(StaffType.STUDENT_SECTION,
                service.createStaff(unified(StaffType.STUDENT_SECTION, null, "student-section@example.com")).staffType());
    }

    @Test
    void unifiedFormCreatesFeeSectionWithoutDepartment() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        stubUnifiedCreation(RoleName.FEE_SECTION);
        assertEquals(StaffType.FEE_SECTION,
                service.createStaff(unified(StaffType.FEE_SECTION, null, "fee-section@example.com")).staffType());
    }

    @Test
    void hodRequiresDepartment() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        assertThrows(BadRequestException.class,
                () -> service.createStaff(unified(StaffType.HOD, null, "hod@example.com")));
    }

    @Test
    void teacherRequiresDepartment() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        assertThrows(BadRequestException.class,
                () -> service.createStaff(unified(StaffType.TEACHER, null, "teacher@example.com")));
    }

    @Test
    void superAdminCannotUseUnifiedStaffForm() {
        authenticate(1L, null, RoleName.SUPER_ADMIN);
        assertThrows(AccessDeniedException.class,
                () -> service.createStaff(unified(StaffType.GENERAL_STAFF, null, "general@example.com")));
    }

    private void stubUnifiedCreation(RoleName roleName) {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(colleges.findById(1L)).thenReturn(Optional.of(college));
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(roles.findByName(roleName)).thenReturn(Optional.of(role(roleName)));
        when(staffProfiles.existsByEmployeeCode(anyString())).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> { User user = invocation.getArgument(0); user.setId(10L); return user; });
        AtomicReference<StaffProfile> saved = new AtomicReference<>();
        when(staffProfiles.save(any(StaffProfile.class))).thenAnswer(invocation -> {
            StaffProfile profile = invocation.getArgument(0); profile.setId(20L); saved.set(profile); return profile;
        });
        when(staffProfiles.findById(20L)).thenAnswer(invocation -> Optional.of(saved.get()));
    }

    private CreateStaffRequest unified(StaffType type, Long departmentId, String email) {
        return new CreateStaffRequest("Mr. Kale", email, "9876543210",
                departmentId, type, null, null, LocalDate.of(2026, 7, 10));
    }

    private Department department(Long id, Long collegeId) {
        Department department = new Department();
        department.setId(id); department.setCollege(college(collegeId, CollegeStatus.ACTIVE));
        department.setName("BCA"); department.setCode("BCA"); department.setStatus(DepartmentStatus.ACTIVE);
        return department;
    }

    private CreateStudentSectionStaffRequest request(Long collegeId) {
        return new CreateStudentSectionStaffRequest(
                collegeId,
                "Student Section Staff",
                "Section@Example.com",
                "9876543210",
                LocalDate.of(2026, 7, 10)
        );
    }

    private StaffProfile staffProfile() {
        User user = new User();
        user.setId(10L);
        user.setFullName("Student Section Staff");
        user.setEmail("section@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role(RoleName.STUDENT_SECTION)));
        StaffProfile profile = new StaffProfile();
        profile.setId(20L);
        profile.setUser(user);
        profile.setCollege(college(1L, CollegeStatus.ACTIVE));
        profile.setEmployeeCode("EMP-ABC001-2026-000001");
        profile.setFullName("Student Section Staff");
        profile.setEmail("section@example.com");
        profile.setStaffType(StaffType.STUDENT_SECTION);
        profile.setStatus(StaffStatus.ACTIVE);
        return profile;
    }

    private College college(Long id, CollegeStatus status) {
        College college = new College();
        college.setId(id);
        college.setName("ABC College");
        college.setCode("ABC001");
        college.setStatus(status);
        return college;
    }

    private Role role(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    private void authenticate(Long userId, Long collegeId, RoleName roleName) {
        User user = new User();
        user.setId(userId);
        user.setCollege(collegeId == null ? null : college(collegeId, CollegeStatus.ACTIVE));
        user.setFullName(roleName.name());
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role(roleName)));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
