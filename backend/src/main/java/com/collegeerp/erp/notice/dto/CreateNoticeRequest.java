package com.collegeerp.erp.notice.dto;

import com.collegeerp.erp.user.entity.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import com.collegeerp.erp.notice.entity.NoticePriority;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 10000) String message,
        NoticePriority priority,
        Set<RoleName> audienceRoles,
        Set<Long> collegeIds,
        Long departmentId,
        NoticeDeliveryMode deliveryMode,
        @Size(max = 500) Set<Long> recipientUserIds
) {}
