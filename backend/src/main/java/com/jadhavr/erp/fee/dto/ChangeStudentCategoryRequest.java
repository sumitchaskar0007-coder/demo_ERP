package com.jadhavr.erp.fee.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ChangeStudentCategoryRequest(@NotBlank @Size(min=2,max=80) String customCategoryName) {}
