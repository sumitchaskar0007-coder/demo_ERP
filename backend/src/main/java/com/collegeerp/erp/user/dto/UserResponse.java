package com.collegeerp.erp.user.dto;

import com.collegeerp.erp.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id, Long collegeId, String collegeName, String collegeCode,
        String fullName, String email, String phone, String profileImageUrl,
        String address, String bio, UserStatus status,
        List<String> roles, LocalDateTime lastLoginAt,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {}
