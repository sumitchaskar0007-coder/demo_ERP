package com.jadhavr.erp.auth.security;

import com.jadhavr.erp.user.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

/** Minimal authorization state cached in Redis. It deliberately excludes passwords and profile data. */
public record AuthorizationSnapshot(
        Long userId,
        Long collegeId,
        String email,
        String fullName,
        UserStatus status,
        LocalDateTime lockedUntil,
        long sessionVersion,
        boolean mustChangePassword,
        List<String> authorities) {
}
