package com.jadhavr.erp.security;

import com.jadhavr.erp.admission.filter.StudentAdmissionAccessFilter;
import com.jadhavr.erp.auth.filter.LoginRateLimitFilter;
import com.jadhavr.erp.auth.security.CustomUserDetailsService;
import com.jadhavr.erp.auth.security.JwtAuthenticationFilter;
import com.jadhavr.erp.common.controller.HealthController;
import com.jadhavr.erp.config.SecurityConfig;
import com.jadhavr.erp.email.controller.AccountEmailController;
import com.jadhavr.erp.email.service.AccountTokenService;
import com.jadhavr.erp.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HealthController.class,
        AccountEmailController.class,
        TestHealthActuatorController.class
})
@Import(SecurityConfig.class)
class PublicEndpointSecurityTest {

    @Autowired private MockMvc mvc;

    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private LoginRateLimitFilter loginRateLimitFilter;
    @MockBean private StudentAdmissionAccessFilter studentAdmissionAccessFilter;
    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private AccountTokenService accountTokenService;
    @MockBean private UserRepository userRepository;

    @BeforeEach
    void allowRequestsThroughApplicationFilters() throws Exception {
        passThrough(jwtAuthenticationFilter);
        passThrough(loginRateLimitFilter);
        passThrough(studentAdmissionAccessFilter);
    }

    @Test
    void healthEndpointsArePublicAndExposeNoDetails() throws Exception {
        for (String path : new String[] {
                "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"
        }) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist());
        }
    }

    @Test
    void sensitiveActuatorEndpointIsNotPublic() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordIsPublicAndEnumerationSafe() throws Exception {
        mvc.perform(post("/api/auth/password/forgot")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for that email, a password reset link will be sent."));

        verify(accountTokenService).forgot("unknown@example.com");
    }

    @Test
    void resetAndVerificationConfirmationArePublic() throws Exception {
        String token = "a".repeat(40);

        mvc.perform(post("/api/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Secure@123\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/email-verification/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void verificationRequestStillRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/auth/email-verification/request").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private void passThrough(jakarta.servlet.Filter filter) throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }
}
