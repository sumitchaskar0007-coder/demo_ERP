package com.jadhavr.erp.auth.dto;

import com.jadhavr.erp.user.entity.UserStatus;
import java.util.List;

public record AuthUserResponse(
        Long id, Long collegeId, String collegeName, String collegeCode,
        String fullName, String email, String phone, UserStatus status, List<String> roles,
        boolean mustChangePassword
) {}
