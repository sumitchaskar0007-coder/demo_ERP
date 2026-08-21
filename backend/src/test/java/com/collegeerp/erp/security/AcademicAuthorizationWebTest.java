package com.collegeerp.erp.security;

import com.collegeerp.erp.academic.controller.AcademicController;
import com.collegeerp.erp.academic.service.AcademicService;
import com.collegeerp.erp.attendance.controller.AttendanceController;
import com.collegeerp.erp.attendance.service.AttendanceService;
import com.collegeerp.erp.auth.security.CustomUserDetailsService;
import com.collegeerp.erp.auth.security.AuthorizationSnapshotService;
import com.collegeerp.erp.auth.security.JwtService;
import com.collegeerp.erp.auth.security.TrustedClientIpResolver;
import com.collegeerp.erp.auth.service.DistributedRateLimiter;
import com.collegeerp.erp.admission.filter.StudentAdmissionAccessFilter;
import com.collegeerp.erp.config.RateLimitProperties;
import com.collegeerp.erp.timetable.controller.TimetableController;
import com.collegeerp.erp.timetable.controller.WeeklyTimetableController;
import com.collegeerp.erp.timetable.service.TimetableService;
import com.collegeerp.erp.timetable.service.WeeklyTimetableService;
import com.collegeerp.erp.user.entity.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AcademicController.class,
        TimetableController.class,
        WeeklyTimetableController.class,
        AttendanceController.class
})
class AcademicAuthorizationWebTest {
    @Autowired private MockMvc mvc;

    @MockBean private AcademicService academicService;
    @MockBean private TimetableService timetableService;
    @MockBean private WeeklyTimetableService weeklyTimetableService;
    @MockBean private AttendanceService attendanceService;
    @MockBean private StringRedisTemplate redis;
    @MockBean private JwtService jwtService;
    @MockBean private AuthorizationSnapshotService authorizationSnapshotService;
    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private DistributedRateLimiter rateLimiter;
    @MockBean private TrustedClientIpResolver clientIpResolver;
    @MockBean private StudentAdmissionAccessFilter studentAdmissionAccessFilter;

    @BeforeEach
    void passThroughAdmissionGateMock() throws Exception {
        when(rateLimiter.check(anyString(), anyString(), anyLong(), any()))
                .thenReturn(new DistributedRateLimiter.Decision(true, 0));
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        doAnswer(invocation -> {
            jakarta.servlet.ServletRequest request = invocation.getArgument(0);
            jakarta.servlet.ServletResponse response = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(studentAdmissionAccessFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousAcademicRequestIs401() throws Exception {
        mvc.perform(get("/api/academic/classes/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotReadAcademicAdministration() throws Exception {
        mvc.perform(get("/api/academic/classes/search")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.STUDENT, 8L, 10L))))
                .andExpect(status().isForbidden());
        verify(academicService, never()).classes(null);
    }

    @Test
    void feeSectionCannotModifyAcademicRecordsAndServiceIsNotCalled() throws Exception {
        mvc.perform(delete("/api/academic/subjects/44")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.FEE_SECTION, 4L, 10L))))
                .andExpect(status().isForbidden());
        verify(academicService, never()).deleteSubject(44L);
    }

    @Test
    void superAdminCannotUseOperationalTimetableApi() throws Exception {
        mvc.perform(get("/api/timetables")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.SUPER_ADMIN, 1L, null))))
                .andExpect(status().isForbidden());
        verify(timetableService, never()).list();
    }

    @Test
    void principalCanReadScopedAcademicSetup() throws Exception {
        when(academicService.classes(null)).thenReturn(List.of());
        mvc.perform(get("/api/academic/classes/search")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.PRINCIPAL, 2L, 10L))))
                .andExpect(status().isOk());
    }

    @Test
    void principalCannotReadTeachingAssignments() throws Exception {
        mvc.perform(get("/api/academic/subject-teacher-assignments")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.PRINCIPAL, 2L, 10L))))
                .andExpect(status().isForbidden());
        verify(academicService, never()).listAssignments(null, null);
    }

    @Test
    void hodCanReadTeachingAssignments() throws Exception {
        when(academicService.listAssignments(null, null)).thenReturn(List.of());
        mvc.perform(get("/api/academic/subject-teacher-assignments")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.HOD, 3L, 10L))))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminCanReadWeeklyTimetableDivisionsForLectureLoadFilters() throws Exception {
        when(weeklyTimetableService.divisions()).thenReturn(List.of());

        mvc.perform(get("/api/weekly-timetables/divisions")
                        .with(authentication(TestSecurityUsers.authentication(RoleName.SUPER_ADMIN, 1L, null))))
                .andExpect(status().isOk());
    }

    @Test
    void subjectTeacherCanReadOnlyTeacherAttendanceSessionList() throws Exception {
        LocalDate today = LocalDate.of(2026, 7, 21);
        when(attendanceService.list(today, today)).thenReturn(List.of());
        mvc.perform(get("/api/attendance/sessions")
                        .param("from", today.toString()).param("to", today.toString())
                        .with(authentication(TestSecurityUsers.authentication(RoleName.SUBJECT_TEACHER, 7L, 10L))))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
        @Bean
        RateLimitProperties rateLimitProperties() {
            return new RateLimitProperties();
        }

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
