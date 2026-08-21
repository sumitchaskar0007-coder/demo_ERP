package com.collegeerp.erp.auth.dto;

import com.collegeerp.erp.user.entity.UserStatus;
import java.util.List;

public record AuthUserResponse(
        Long id, Long collegeId, Long institutionId, String collegeName, String collegeCode,
        String fullName, String email, String phone, String profileImageUrl,
        String address, String bio, UserStatus status, List<String> roles,
        boolean mustChangePassword, boolean emailVerified
) {}
