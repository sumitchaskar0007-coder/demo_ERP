package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.SubmitAdmissionRequest;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.mapper.AdmissionMapper;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.RoleRepository;
import com.jadhavr.erp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplTest {

    @Mock private CollegeRepository collegeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private AdmissionFormRepository admissionFormRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AdmissionServiceImpl(
                collegeRepository,
                departmentRepository,
                userRepository,
                roleRepository,
                studentProfileRepository,
                admissionFormRepository,
                passwordEncoder,
                new AdmissionMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPublicAdmissionInfoSucceedsWithOnlyActiveDepartments() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findByCollegeIdAndStatus(1L, DepartmentStatus.ACTIVE))
                .thenReturn(List.of(department(10L, college, DepartmentStatus.ACTIVE)));

        var result = service.getPublicAdmissionInfo("abc001");

        assertEquals("ABC001", result.collegeCode());
        assertEquals(1, result.departments().size());
        assertEquals("BCA", result.departments().get(0).code());
        verify(departmentRepository).findByCollegeIdAndStatus(1L, DepartmentStatus.ACTIVE);
    }

    @Test
    void getPublicAdmissionInfoFailsWhenCollegeNotFound() {
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPublicAdmissionInfo("ABC001"));
    }

    @Test
    void getPublicAdmissionInfoFailsWhenCollegeInactive() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.getPublicAdmissionInfo("ABC001"));
    }

    @Test
    void submitAdmissionSucceedsAndCreatesStudentUserProfileAndAdmission() {
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college, DepartmentStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.existsByEmail("aarav.patil@example.com")).thenReturn(false);
        when(admissionFormRepository.existsByEmailAndCollegeIdAndStatusNotIn(
                anyString(), any(), any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole()));
        when(studentProfileRepository.existsByAdmissionNumber(anyString())).thenReturn(false);
        when(admissionFormRepository.existsByAdmissionReferenceNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(20L);
            return user;
        });
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> {
            StudentProfile profile = invocation.getArgument(0);
            profile.setId(30L);
            return profile;
        });
        when(admissionFormRepository.save(any(AdmissionForm.class))).thenAnswer(invocation -> {
            AdmissionForm form = invocation.getArgument(0);
            form.setId(40L);
            return form;
        });

        var result = service.submitAdmission("abc001", request());

        assertEquals(AdmissionStatus.SUBMITTED, result.status());
        assertEquals(20L, result.studentUserId());
        assertEquals(30L, result.studentProfileId());
        assertTrue(result.admissionReferenceNumber().startsWith("ADM-ABC001-"));
        assertTrue(result.admissionNumber().startsWith("STU-ABC001-"));
        assertEquals("9876543210", result.temporaryPassword());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("aarav.patil@example.com", savedUser.getEmail());
        assertEquals(RoleName.STUDENT, savedUser.getRoles().iterator().next().getName());
        assertNotEquals(result.temporaryPassword(), savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(result.temporaryPassword(), savedUser.getPasswordHash()));
        assertTrue(savedUser.isMustChangePassword());

        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        assertEquals(StudentStatus.ADMISSION_SUBMITTED, profileCaptor.getValue().getStatus());
        assertEquals(StudentCategory.SC, profileCaptor.getValue().getStudentCategory());

        ArgumentCaptor<AdmissionForm> admissionCaptor = ArgumentCaptor.forClass(AdmissionForm.class);
        verify(admissionFormRepository).save(admissionCaptor.capture());
        assertEquals(AdmissionStatus.SUBMITTED, admissionCaptor.getValue().getStatus());
        assertEquals(StudentCategory.SC, admissionCaptor.getValue().getStudentCategory());
    }

    @Test
    void submitAdmissionFailsWhenCollegeNotFound() {
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAdmission("ABC001", request()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitAdmissionFailsWhenCollegeInactive() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitAdmissionFailsWhenDepartmentNotFound() {
        when(collegeRepository.findByCode("ABC001"))
                .thenReturn(Optional.of(college(1L, CollegeStatus.ACTIVE)));
        when(departmentRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenDepartmentBelongsToAnotherCollege() {
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college(2L, CollegeStatus.ACTIVE), DepartmentStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenDepartmentInactive() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department(10L, college, DepartmentStatus.INACTIVE)));

        assertThrows(BadRequestException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void submitAdmissionFailsWhenUserEmailAlreadyExists() {
        College college = college(1L, CollegeStatus.ACTIVE);
        when(collegeRepository.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(departmentRepository.findById(10L))
                .thenReturn(Optional.of(department(10L, college, DepartmentStatus.ACTIVE)));
        when(userRepository.existsByEmail("aarav.patil@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.submitAdmission("ABC001", request()));
    }

    @Test
    void getMyLatestAdmissionFailsWhenAdmissionNotFound() {
        authenticateStudent(20L);
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        when(studentProfileRepository.findByUserId(20L)).thenReturn(Optional.of(profile));
        when(admissionFormRepository.findTopByStudentIdOrderByCreatedAtDesc(30L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyLatestAdmission());
    }

    @Test
    void getMyLatestAdmissionFailsWhenStudentProfileNotFound() {
        authenticateStudent(20L);
        when(studentProfileRepository.findByUserId(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyLatestAdmission());
        verify(admissionFormRepository, never()).findTopByStudentIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getMyLatestAdmissionSucceeds() {
        authenticateStudent(20L);
        College college = college(1L, CollegeStatus.ACTIVE);
        Department department = department(10L, college, DepartmentStatus.ACTIVE);
        User user = new User();
        user.setId(20L);
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        AdmissionForm admission = new AdmissionForm();
        admission.setId(40L);
        admission.setAdmissionReferenceNumber("ADM-ABC001-2026-000001");
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(profile);
        admission.setStudentUser(user);
        admission.setAcademicYear("2026-2027");
        admission.setFullName("Aarav Rajesh Patil");
        admission.setEmail("aarav.patil@example.com");
        admission.setPhone("9876543210");
        admission.setDateOfBirth(LocalDate.of(2007, 5, 14));
        admission.setGender("Male");
        admission.setParentName("Rajesh Patil");
        admission.setParentPhone("9876500001");
        admission.setStatus(AdmissionStatus.SUBMITTED);
        when(studentProfileRepository.findByUserId(20L)).thenReturn(Optional.of(profile));
        when(admissionFormRepository.findTopByStudentIdOrderByCreatedAtDesc(30L))
                .thenReturn(Optional.of(admission));

        var result = service.getMyLatestAdmission();

        assertEquals(40L, result.id());
        assertEquals("ADM-ABC001-2026-000001", result.admissionReferenceNumber());
        assertEquals("STU-ABC001-2026-000001", result.admissionNumber());
    }

    private SubmitAdmissionRequest request() {
        return new SubmitAdmissionRequest(
                10L,
                StudentCategory.SC,
                "Aarav",
                "Rajesh",
                "Patil",
                "Aarav.Patil@Example.com",
                "9876543210",
                LocalDate.of(2007, 5, 14),
                "Male",
                "Shivaji Nagar",
                "Near Bus Stand",
                "Pune",
                "Maharashtra",
                "411001",
                "Rajesh Patil",
                "9876500001",
                "Rajesh.Patil@Example.com",
                "ABC Junior College",
                "12th Science",
                new BigDecimal("78.50")
        );
    }

    private College college(Long id, CollegeStatus status) {
        College college = new College();
        college.setId(id);
        college.setName("ABC College of Computer Science");
        college.setCode("ABC001");
        college.setStatus(status);
        return college;
    }

    private Department department(Long id, College college, DepartmentStatus status) {
        Department department = new Department();
        department.setId(id);
        department.setCollege(college);
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");
        department.setStatus(status);
        return department;
    }

    private Role studentRole() {
        Role role = new Role();
        role.setId(7L);
        role.setName(RoleName.STUDENT);
        return role;
    }

    private void authenticateStudent(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Aarav Patil");
        user.setEmail("aarav.patil@example.com");
        user.setPasswordHash("hash");
        user.setStatus(com.jadhavr.erp.user.entity.UserStatus.ACTIVE);
        user.setRoles(Set.of(studentRole()));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
