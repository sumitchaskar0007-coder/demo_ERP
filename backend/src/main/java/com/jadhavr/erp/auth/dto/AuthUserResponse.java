package com.jadhavr.erp.auth.dto;

import com.jadhavr.erp.user.entity.UserStatus;
import java.util.List;

public record AuthUserResponse(
        Long id, Long collegeId, Long institutionId, String collegeName, String collegeCode,
        String fullName, String email, String phone, String profileImageUrl,
        String address, String bio, UserStatus status, List<String> roles
) {}
