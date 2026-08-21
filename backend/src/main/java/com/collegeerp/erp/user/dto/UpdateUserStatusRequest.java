package com.collegeerp.erp.user.dto;

import com.collegeerp.erp.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required") UserStatus status
) {}
