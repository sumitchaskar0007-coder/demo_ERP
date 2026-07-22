package com.jadhavr.erp.notice.dto;

import com.jadhavr.erp.user.entity.RoleName;
import java.time.LocalDateTime;
import java.util.Set;
import com.jadhavr.erp.notice.entity.NoticePriority;

public record NoticeResponse(
        Long id, String title, String message, NoticePriority priority, boolean acknowledged,
        Long createdByUserId, String createdByName,
        Set<Long> collegeIds, Set<String> collegeNames, boolean allColleges,
        Long departmentId, String departmentName,
        Set<RoleName> audienceRoles, LocalDateTime createdAt
) {}
