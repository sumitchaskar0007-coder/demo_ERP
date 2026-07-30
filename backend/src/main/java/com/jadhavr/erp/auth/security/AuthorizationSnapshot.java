package com.jadhavr.erp.auth.security;

import com.jadhavr.erp.user.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Only fields required to authorize a request are stored in Redis. */
public record AuthorizationSnapshot(
        Long userId,
        Long collegeId,
        String email,
        UserStatus status,
        LocalDateTime lockedUntil,
        long sessionVersion,
        List<String> authorities) {
}
