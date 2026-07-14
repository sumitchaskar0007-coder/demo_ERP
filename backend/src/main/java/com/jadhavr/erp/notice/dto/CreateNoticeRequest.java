package com.jadhavr.erp.notice.dto;

import com.jadhavr.erp.user.entity.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateNoticeRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 10000) String message,
        @NotEmpty Set<RoleName> audienceRoles,
        Set<Long> collegeIds
) {}
