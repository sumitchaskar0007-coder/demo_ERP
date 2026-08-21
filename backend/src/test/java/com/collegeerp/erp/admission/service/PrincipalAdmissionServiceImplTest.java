package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.entity.AdmissionStatusHistory;
import com.collegeerp.erp.admission.enums.AdmissionAction;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.admission.mapper.AdmissionStatusHistoryMapper;
import com.collegeerp.erp.admission.mapper.StudentSectionAdmissionMapper;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.admission.repository.AdmissionStatusHistoryRepository;
import com.collegeerp.erp.auth.security.CustomUserDetails;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.student.entity.StudentProfile;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrincipalAdmissionServiceImplTest {
    @Mock private AdmissionFormRepository admissions;
    @Mock private AdmissionStatusHistoryRepository histories;

    private PrincipalAdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PrincipalAdmissionServiceImpl(
                admissions,
                histories,
                new StudentSectionAdmissionMapper(),
                new AdmissionStatusHistoryMapper()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void principalGetsReviewReadyAdmissionsForOwnCollege() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        when(admissions.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admission(100L, 1L))));

        var result = service.getReviewReadyAdmissions(null, null, 0, 10, "createdAt", "desc");

        assertEquals(1, result.totalElements());
        assertEquals(AdmissionStatus.STUDENT_SECTION_APPROVED, result.content().get(0).status());
    }

    @Test
    void principalCannotAccessAnotherCollegeAdmission() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission(100L, 2L)));

        assertThrows(AccessDeniedException.class, () -> service.getAdmissionForPrincipal(100L));
    }

    @Test
    void principalHistorySucceeds() {
        authenticate(2L, 1L, RoleName.PRINCIPAL);
        AdmissionForm admission = admission(100L, 1L);
        AdmissionStatusHistory history = new AdmissionStatusHistory();
        history.setId(1L);
        history.setAdmissionForm(admission);
        history.setAction(AdmissionAction.STUDENT_SECTION_APPROVED);
        history.setNewStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        when(admissions.findById(100L)).thenReturn(Optional.of(admission));
        when(histories.findByAdmissionFormIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(history));

        var result = service.getAdmissionHistoryForPrincipal(100L);

        assertEquals(1, result.size());
        assertEquals(AdmissionAction.STUDENT_SECTION_APPROVED, result.get(0).action());
    }

    private AdmissionForm admission(Long id, Long collegeId) {
        College college = new College();
        college.setId(collegeId);
        college.setName("ABC College");
        college.setCode("ABC001");
        Department department = new Department();
        department.setId(10L);
        department.setCollege(college);
        department.setName("Bachelor of Computer Applications");
        department.setCode("BCA");
        StudentProfile profile = new StudentProfile();
        profile.setId(30L);
        profile.setAdmissionNumber("STU-ABC001-2026-000001");
        AdmissionForm admission = new AdmissionForm();
        admission.setId(id);
        admission.setAdmissionReferenceNumber("ADM-ABC001-2026-000001");
        admission.setCollege(college);
        admission.setDepartment(department);
        admission.setStudent(profile);
        admission.setStudentUser(user(60L, collegeId, RoleName.STUDENT));
        admission.setAcademicYear("2026-2027");
        admission.setFullName("Aarav Patil");
        admission.setEmail("aarav@example.com");
        admission.setPhone("9876543210");
        admission.setDateOfBirth(LocalDate.of(2007, 5, 14));
        admission.setGender("Male");
        admission.setParentName("Rajesh Patil");
        admission.setParentPhone("9876500001");
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        admission.setPrintCount(0);
        return admission;
    }

    private User user(Long id, Long collegeId, RoleName roleName) {
        User user = new User();
        user.setId(id);
        College college = new College();
        college.setId(collegeId);
        user.setCollege(college);
        user.setFullName(roleName.name());
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private void authenticate(Long userId, Long collegeId, RoleName roleName) {
        User user = user(userId, collegeId, roleName);
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
