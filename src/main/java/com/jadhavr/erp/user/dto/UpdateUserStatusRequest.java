package com.jadhavr.erp.user.dto;

import com.jadhavr.erp.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required") UserStatus status
) {}
