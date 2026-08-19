package com.jadhavr.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.common.api.ErrorResponse;
import com.jadhavr.erp.auth.security.CustomUserDetailsService;
import com.jadhavr.erp.auth.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.jadhavr.erp.auth.filter.LoginRateLimitFilter;
import com.jadhavr.erp.auth.filter.TemporaryPasswordAccessFilter;
import com.jadhavr.erp.admission.filter.StudentAdmissionAccessFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final TemporaryPasswordAccessFilter temporaryPasswordAccessFilter;
    private final StudentAdmissionAccessFilter studentAdmissionAccessFilter;
    private final List<String> allowedOrigins;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public SecurityConfig(ObjectMapper objectMapper,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService, LoginRateLimitFilter loginRateLimitFilter,
                          TemporaryPasswordAccessFilter temporaryPasswordAccessFilter,
                          StudentAdmissionAccessFilter studentAdmissionAccessFilter,
                          @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
                          @Value("${app.auth.cookie-secure:false}") boolean cookieSecure,
                          @Value("${app.auth.cookie-same-site:Strict}") String cookieSameSite) {
        this.objectMapper = objectMapper;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.temporaryPasswordAccessFilter = temporaryPasswordAccessFilter;
        this.studentAdmissionAccessFilter = studentAdmissionAccessFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> {
                    CookieCsrfTokenRepository repository =
                            csrfTokenRepository(cookieSecure, cookieSameSite);
                    CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
                    handler.setCsrfRequestAttributeName("_csrf");
                    csrf.csrfTokenRepository(repository).csrfTokenRequestHandler(handler)
                            .ignoringRequestMatchers("/api/v1/auth/login", "/api/public/admissions/**");
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
                        .contentTypeOptions(content -> {}))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").denyAll()
                        .requestMatchers(
                                "/actuator/health/**",
                                "/api/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/csrf",
                                "/api/public/admissions/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/password/forgot",
                                "/api/auth/password/reset",
                                "/api/auth/email-verification/confirm"
                        ).permitAll()
                        .requestMatchers("/api/dashboard/**")
                                .hasAnyRole("SUPER_ADMIN", "PRINCIPAL", "HOD", "STUDENT_SECTION",
                                        "FEE_SECTION", "CLASS_TEACHER", "SUBJECT_TEACHER", "STUDENT")
                        .requestMatchers("/api/reports/**")
                                .hasAnyRole("SUPER_ADMIN", "PRINCIPAL", "HOD", "STUDENT_SECTION", "FEE_SECTION")
                        .requestMatchers("/api/audit-logs/**").hasAnyRole("SUPER_ADMIN", "PRINCIPAL")
                        .requestMatchers("/api/academic/**")
                                .hasAnyRole("PRINCIPAL", "HOD", "CLASS_TEACHER", "SUBJECT_TEACHER")
                        .requestMatchers("/api/timetables/**").hasRole("PRINCIPAL")
                        .requestMatchers(HttpMethod.GET, "/api/weekly-timetables/divisions")
                                .hasAnyRole("SUPER_ADMIN", "PRINCIPAL", "HOD", "CLASS_TEACHER")
                        .requestMatchers("/api/weekly-timetables/**")
                                .hasAnyRole("PRINCIPAL", "HOD", "CLASS_TEACHER")
                        .requestMatchers("/api/attendance/**")
                                .hasAnyRole("PRINCIPAL", "HOD", "CLASS_TEACHER", "SUBJECT_TEACHER", "STUDENT")
                        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/college-settings/**").hasAnyRole("SUPER_ADMIN", "PRINCIPAL")
                        .requestMatchers(HttpMethod.GET, "/api/admission-document-requirements/**")
                                .hasAnyRole("STUDENT", "STUDENT_SECTION", "PRINCIPAL", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/student-section/**").hasAnyRole("STUDENT_SECTION", "PRINCIPAL", "SUPER_ADMIN")
                        .requestMatchers("/api/student-section/**").hasAnyRole("STUDENT_SECTION", "SUPER_ADMIN")
                        .requestMatchers("/api/principal/staff/**").hasAnyRole("PRINCIPAL", "SUPER_ADMIN")
                        .requestMatchers("/api/principal/admissions/**").hasAnyRole("PRINCIPAL", "SUPER_ADMIN")
                        .requestMatchers("/api/principal/students/*/fee-adjustments/**").hasRole("PRINCIPAL")
                        .requestMatchers("/api/principal/**").hasAnyRole("PRINCIPAL", "SUPER_ADMIN")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/notices/**").authenticated()
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout", "/api/auth/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loginRateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(temporaryPasswordAccessFilter, LoginRateLimitFilter.class)
                .addFilterAfter(studentAdmissionAccessFilter, TemporaryPasswordAccessFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Access denied", request.getRequestURI()))
                )
                .build();
    }

    static CookieCsrfTokenRepository csrfTokenRepository(boolean secure, String sameSite) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.secure(secure).sameSite(sameSite));
        return repository;
    }

    /*
     * These filters are positioned explicitly inside Spring Security above.
     * Disable servlet-container auto-registration so they cannot execute once
     * before the security chain (where the authenticated principal is not yet
     * available) and then a second time inside it.
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration() {
        FilterRegistrationBean<LoginRateLimitFilter> registration =
                new FilterRegistrationBean<>(loginRateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TemporaryPasswordAccessFilter> temporaryPasswordAccessFilterRegistration() {
        FilterRegistrationBean<TemporaryPasswordAccessFilter> registration =
                new FilterRegistrationBean<>(temporaryPasswordAccessFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<StudentAdmissionAccessFilter> studentAdmissionAccessFilterRegistration() {
        FilterRegistrationBean<StudentAdmissionAccessFilter> registration =
                new FilterRegistrationBean<>(studentAdmissionAccessFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE
                , "X-XSRF-TOKEN"
        ));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeSecurityError(HttpServletResponse response, int status, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message, path));
    }
}
