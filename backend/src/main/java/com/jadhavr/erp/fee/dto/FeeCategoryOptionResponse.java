package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;

public record FeeCategoryOptionResponse(StudentCategory category, String customCategoryName, String label) {}
