package com.jadhavr.erp.admission.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.CustomUserDetails;
import com.jadhavr.erp.common.api.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Server-side admission gate. Frontend redirects are never trusted as authorization. */
@Component
public class StudentAdmissionAccessFilter extends OncePerRequestFilter {
    private static final Set<AdmissionStatus> UNLOCKED = Set.of(
            AdmissionStatus.STUDENT_SECTION_APPROVED,
            AdmissionStatus.PRINCIPAL_REVIEW_PENDING,
            AdmissionStatus.PRINCIPAL_APPROVED);

    private final AdmissionFormRepository admissions;
    private final ObjectMapper objectMapper;

    public StudentAdmissionAccessFilter(AdmissionFormRepository admissions, ObjectMapper objectMapper) {
        this.admissions = admissions;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/")
                || path.startsWith("/api/student/admissions/")
                || path.equals("/api/admission-document-requirements/me")
                || path.startsWith("/api/v1/auth/")
                || path.equals("/api/auth/change-password")
                || path.startsWith("/api/auth/password/")
                || path.startsWith("/api/auth/email-verification/")
                || path.equals("/api/account/change-password")
                || path.equals("/api/health")
                || path.startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user)
                || user.getAuthorities().stream().noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))) {
            chain.doFilter(request, response);
            return;
        }

        AdmissionForm admission = admissions.findTopByStudentUserIdOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        if (admission != null && UNLOCKED.contains(admission.getStatus())) {
            if (user.isMustChangePassword()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                        "Change your temporary password before accessing student features",
                        request.getRequestURI()));
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String message = admission != null
                && (admission.getStatus() == AdmissionStatus.STUDENT_SECTION_REJECTED
                    || admission.getStatus() == AdmissionStatus.PRINCIPAL_REJECTED)
                ? "Admission was rejected. Correct and resubmit the admission form before continuing"
                : "Complete and submit the admission form before accessing student features";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message, request.getRequestURI()));
    }
}
