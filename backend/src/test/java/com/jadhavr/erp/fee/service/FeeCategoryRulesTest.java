package com.jadhavr.erp.fee.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.fee.enums.StudentCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeeCategoryRulesTest {
    @Test
    void normalizesConfiguredOtherCategory() {
        assertEquals("NT-C", FeeCategoryRules.normalizeCustomCategory(StudentCategory.OTHER, " nt-c "));
    }

    @Test
    void rejectsMissingOrReservedOtherCategory() {
        assertThrows(BadRequestException.class,
                () -> FeeCategoryRules.normalizeCustomCategory(StudentCategory.OTHER, " "));
        for (StudentCategory category : StudentCategory.values()) {
            assertThrows(BadRequestException.class,
                    () -> FeeCategoryRules.normalizeCustomCategory(
                            StudentCategory.OTHER, category.name()));
        }
    }

    @Test
    void appliesTheSameRulesToEveryStandardCategory() {
        for (StudentCategory category : StudentCategory.values()) {
            if (category == StudentCategory.OTHER) continue;
            assertNull(FeeCategoryRules.normalizeCustomCategory(category, null));
            assertThrows(BadRequestException.class,
                    () -> FeeCategoryRules.normalizeCustomCategory(category, "NT"));
        }
    }

    @Test
    void academicYearVariantsAreCanonicalAndShort() {
        assertEquals(
                java.util.List.of("2026-2027", "2026-27"),
                FeeCategoryRules.academicYearVariants("2026/27"));
    }
}
