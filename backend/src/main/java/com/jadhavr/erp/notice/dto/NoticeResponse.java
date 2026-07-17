package com.jadhavr.erp.notice.dto;

import com.jadhavr.erp.user.entity.RoleName;
import java.time.LocalDateTime;
import java.util.Set;

public record NoticeResponse(
        Long id, String title, String message, Long createdByUserId, String createdByName,
        Set<Long> collegeIds, Set<String> collegeNames, Long departmentId, String departmentName,
        Set<RoleName> audienceRoles, LocalDateTime createdAt
) {}
