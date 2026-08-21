package com.collegeerp.erp.academic.dto;

import jakarta.validation.constraints.NotNull;

public record AssignClassTeacherRequest(@NotNull Long staffProfileId) {
}
