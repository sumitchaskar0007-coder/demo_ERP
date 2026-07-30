package com.jadhavr.erp.security;

import com.jadhavr.erp.academic.repository.AcademicClassRepository;
import com.jadhavr.erp.academic.repository.AttendanceSessionRepository;
import com.jadhavr.erp.academic.repository.SectionRepository;
import com.jadhavr.erp.academic.repository.SubjectRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.analytics.controller.DashboardController;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.repository.DepartmentRepository;
import com.jadhavr.erp.fee.dto.FeeBalanceTotals;
import com.jadhavr.erp.fee.repository.FeePaymentRepository;
import com.jadhavr.erp.fee.repository.StudentFeeAccountRepository;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.student.enums.StudentStatus;
import com.jadhavr.erp.student.repository.StudentProfileRepository;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.repository.UserRepository;
import com.jadhavr.erp.auth.security.CustomUserDetailsService;
import com.jadhavr.erp.auth.security.AuthorizationSnapshotService;
import com.jadhavr.erp.auth.security.JwtService;
import com.jadhavr.erp.auth.security.TrustedClientIpResolver;
import com.jadhavr.erp.auth.service.DistributedRateLimiter;
import com.jadhavr.erp.admission.filter.StudentAdmissionAccessFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardAuthorizationWebTest {
    @Autowired private MockMvc mvc;

    @MockBean private CollegeRepository colleges;
    @MockBean private DepartmentRepository departments;
    @MockBean private UserRepository users;
    @MockBean private StudentProfileRepository students;
    @MockBean private StaffProfileRepository staff;
    @MockBean private AdmissionFormRepository admissions;
    @MockBean private StudentFeeAccountRepository fees;
    @MockBean private FeePaymentRepository payments;
    @MockBean private AcademicClassRepository classes;
    @MockBean private SectionRepository sections;
    @MockBean private SubjectRepository subjects;
    @MockBean private AttendanceSessionRepository attendance;
    @MockBean private StringRedisTemplate redis;
    @MockBean private JwtService jwtService;
    @MockBean private AuthorizationSnapshotService authorizationSnapshotService;
    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private DistributedRateLimiter rateLimiter;
    @MockBean private TrustedClientIpResolver clientIpResolver;
    @MockBean private StudentAdmissionAccessFilter studentAdmissionAccessFilter;

    @BeforeEach
    void setUpResponses() {
        when(rateLimiter.check(anyString(), anyString(), anyLong(), any()))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        try {
            doAnswer(invocation -> {
                jakarta.servlet.ServletRequest request = invocation.getArgument(0);
                jakarta.servlet.ServletResponse response = invocation.getArgument(1);
                jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                chain.doFilter(request, response);
                return null;
            }).when(studentAdmissionAccessFilter).doFilter(any(), any(), any());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        FeeBalanceTotals zero = new FeeBalanceTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        when(fees.balanceTotals()).thenReturn(zero);
        when(fees.balanceTotalsByCollegeId(anyLong())).thenReturn(zero);

        College college = new College();
        college.setId(10L);
        college.setName("Scoped College");
        Department department = new Department();
        department.setId(20L);
        department.setName("Scoped Department");
        department.setCollege(college);

        StaffProfile hod = new StaffProfile();
        hod.setId(200L);
        hod.setCollege(college);
        hod.setDepartment(department);

        StudentProfile student = new StudentProfile();
        student.setId(300L);
        student.setFullName("Scoped Student");
        student.setCollege(college);
        student.setDepartment(department);
        student.setStatus(StudentStatus.ACTIVE);

        when(staff.findByUserId(5L)).thenReturn(Optional.of(hod));
        when(students.findByUserId(8L)).thenReturn(Optional.of(student));
    }

    @Test
    void unauthenticatedRequestIs401() throws Exception {
        mvc.perform(get("/api/dashboard/super-admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIs403() throws Exception {
        mvc.perform(get("/api/dashboard/super-admin")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.STUDENT, 8L, 10L))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "SUPER_ADMIN,1,/api/dashboard/super-admin",
            "PRINCIPAL,2,/api/dashboard/principal",
            "STUDENT_SECTION,3,/api/dashboard/student-section",
            "FEE_SECTION,4,/api/dashboard/fee-section",
            "HOD,5,/api/dashboard/hod",
            "CLASS_TEACHER,6,/api/dashboard/teacher",
            "SUBJECT_TEACHER,7,/api/dashboard/teacher",
            "STUDENT,8,/api/dashboard/student"
    })
    void eachRoleCanAccessOnlyItsDashboard(RoleName role, long userId, String path) throws Exception {
        Long collegeId = role == RoleName.SUPER_ADMIN ? null : 10L;
        mvc.perform(get(path).with(authentication(TestSecurityUsers.authentication(role, userId, collegeId))))
                .andExpect(status().isOk());
    }

    @Test
    void principalCannotRequestDifferentCollege() throws Exception {
        mvc.perform(get("/api/dashboard/principal").param("collegeId", "99")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.PRINCIPAL, 2L, 10L))))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
        @Bean
        SecurityFilterChain testFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
                throws Exception {
            AuthenticationEntryPoint unauthorized = (request, response, exception) -> response.sendError(401);
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
                    .httpBasic(withDefaults())
                    .build();
        }
    }
}
