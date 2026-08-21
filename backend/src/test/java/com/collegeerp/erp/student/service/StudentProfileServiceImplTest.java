package com.collegeerp.erp.student.service;

import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.student.entity.StudentProfile;
import com.collegeerp.erp.student.enums.StudentStatus;
import com.collegeerp.erp.student.mapper.StudentProfileMapper;
import com.collegeerp.erp.student.repository.StudentProfileRepository;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import com.collegeerp.erp.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileServiceImplTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    private StudentProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentProfileServiceImpl(
                studentProfileRepository,
                new StudentProfileMapper()
        );
        authenticateStudent(20L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfileSucceeds() {
        when(studentProfileRepository.findByUserId(20L))
                .thenReturn(Optional.of(profile(30L, 20L)));

        var result = service.getMyProfile();

        assertEquals(30L, result.id());
        assertEquals(20L, result.userId());
        assertEquals("STU-ABC001-2026-000001", result.admissionNumber());
        assertEquals(StudentStatus.ADMISSION_SUBMITTED, result.status());
    }

    @Test
    void getMyProfileFailsIfProfileNotFound() {
        when(studentProfileRepository.findByUserId(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyProfile());
    }

    private void authenticateStudent(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Aarav Patil");
        user.setEmail("aarav.patil@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(RoleName.STUDENT);
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private StudentProfile profile(Long id, Long userId) {
        College college = new College();
        college.setId(1L);
        college.setName("ABC College");
        college.setCode("ABC001");

        Department department = new Department();
        department.setId(10L);
        department.setCollege(college);
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");

        User user = new User();
        user.setId(userId);

        StudentProfile profile = new StudentProfile();
        profile.setId(id);
        profile.setUser(user);
        profile.setCollege(college);
        profile.setDepartment(department);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        profile.setFullName("Aarav Patil");
        profile.setEmail("aarav.patil@example.com");
        profile.setPhone("9876543210");
        profile.setDateOfBirth(LocalDate.of(2007, 5, 14));
        profile.setGender("Male");
        profile.setParentName("Rajesh Patil");
        profile.setParentPhone("9876500001");
        profile.setStatus(StudentStatus.ADMISSION_SUBMITTED);
        return profile;
    }
}
