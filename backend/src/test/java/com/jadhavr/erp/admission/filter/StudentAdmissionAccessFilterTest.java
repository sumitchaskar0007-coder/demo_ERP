package com.jadhavr.erp.admission.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAdmissionAccessFilterTest {
    @Mock private AdmissionFormRepository admissions;
    @Mock private FilterChain chain;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksProtectedApiForStudentWhoseAdmissionIsPending() throws Exception {
        authenticateStudent(21L);
        AdmissionForm admission = new AdmissionForm();
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING);
        when(admissions.findTopByStudentUserIdOrderByCreatedAtDesc(21L))
                .thenReturn(Optional.of(admission));
        MockHttpServletRequest request = request("/api/student/fees/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Complete and submit"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsProtectedApiAfterStudentSectionApproval() throws Exception {
        authenticateStudent(21L);
        AdmissionForm admission = new AdmissionForm();
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        when(admissions.findTopByStudentUserIdOrderByCreatedAtDesc(21L))
                .thenReturn(Optional.of(admission));
        MockHttpServletRequest request = request("/api/student/fees/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void blocksDashboardApiAfterApprovalUntilTemporaryPasswordIsChanged() throws Exception {
        authenticateStudent(21L, true);
        AdmissionForm admission = new AdmissionForm();
        admission.setStatus(AdmissionStatus.STUDENT_SECTION_APPROVED);
        when(admissions.findTopByStudentUserIdOrderByCreatedAtDesc(21L))
                .thenReturn(Optional.of(admission));
        MockHttpServletRequest request = request("/api/student/fees/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Change your temporary password"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void alwaysAllowsStudentOwnedAdmissionEndpoints() throws Exception {
        authenticateStudent(21L);
        MockHttpServletRequest request = request("/api/student/admissions/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(admissions, never()).findTopByStudentUserIdOrderByCreatedAtDesc(21L);
    }

    private StudentAdmissionAccessFilter filter() {
        return new StudentAdmissionAccessFilter(
                admissions, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    private void authenticateStudent(Long id) {
        authenticateStudent(id, false);
    }

    private void authenticateStudent(Long id, boolean mustChangePassword) {
        Role role = new Role();
        role.setName(RoleName.STUDENT);
        User user = new User();
        user.setId(id);
        user.setEmail("student@example.com");
        user.setPasswordHash("hash");
        user.setFullName("Student");
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(mustChangePassword);
        user.setRoles(Set.of(role));
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
