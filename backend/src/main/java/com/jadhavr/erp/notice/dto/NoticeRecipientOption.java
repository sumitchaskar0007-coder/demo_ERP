package com.jadhavr.erp.notice.dto;

import com.jadhavr.erp.user.entity.RoleName;
import java.util.Set;

public record NoticeRecipientOption(
        Long userId,
        String fullName,
        String email,
        Long collegeId,
        String collegeName,
        Long departmentId,
        String departmentName,
        Set<RoleName> roles
) {}
