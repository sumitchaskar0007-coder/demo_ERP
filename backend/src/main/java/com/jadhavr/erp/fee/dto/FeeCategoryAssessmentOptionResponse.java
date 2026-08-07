package com.jadhavr.erp.fee.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import java.math.BigDecimal;

public record FeeCategoryAssessmentOptionResponse(
        Long feeStructureId,
        StudentCategory studentCategory,
        String customCategoryName,
        String label,
        String gender,
        BigDecimal totalFee,
        BigDecimal scholarshipAmount,
        BigDecimal payableFee) {
}
