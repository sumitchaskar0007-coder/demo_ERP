package com.collegeerp.erp.auth.security;

import com.collegeerp.erp.common.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static CustomUserDetails requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new BadRequestException("Authenticated user is invalid");
        }
        return details;
    }

    public static String getCurrentUserEmail() {
        return requireCurrentUser().getUsername();
    }

    public static Long getCurrentUserId() {
        return requireCurrentUser().getId();
    }

    public static boolean hasRole(String roleName) {
        String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return requireCurrentUser().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    public static boolean isPrincipal() {
        return hasRole("PRINCIPAL");
    }

    public static boolean isStudentSection() {
        return hasRole("STUDENT_SECTION");
    }
}
