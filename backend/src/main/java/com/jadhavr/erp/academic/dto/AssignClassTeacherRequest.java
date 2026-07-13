package com.jadhavr.erp.academic.dto;

import jakarta.validation.constraints.NotNull;

public record AssignClassTeacherRequest(@NotNull Long staffProfileId) {
}
