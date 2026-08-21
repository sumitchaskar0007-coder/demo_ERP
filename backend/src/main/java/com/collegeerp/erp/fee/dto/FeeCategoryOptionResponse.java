package com.collegeerp.erp.fee.dto;

import com.collegeerp.erp.fee.enums.StudentCategory;

public record FeeCategoryOptionResponse(StudentCategory category, String customCategoryName, String label) {}
