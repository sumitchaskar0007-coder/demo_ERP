package com.collegeerp.erp.fee.repository;

import com.collegeerp.erp.fee.entity.FeeStructure;
import com.collegeerp.erp.fee.enums.FeeStructureStatus;
import com.collegeerp.erp.fee.enums.StudentCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeeStructureRepositoryTest {
    private FeeStructureRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(FeeStructureRepository.class, CALLS_REAL_METHODS);
    }

    @Test
    void standardCategoryUsesTheNullSafeQuery() {
        FeeStructure configured = new FeeStructure();
        List<String> academicYears = List.of("2026-2027", "2026-27");
        when(repository.findAllConfiguredAssessmentsWithoutCustomCategory(
                1L, 2L, academicYears, StudentCategory.VJNT,
                "MALE", "First Year", FeeStructureStatus.ACTIVE))
                .thenReturn(List.of(configured));

        assertThat(repository.findConfiguredAssessments(
                1L, 2L, academicYears, StudentCategory.VJNT, null,
                "MALE", "First Year", FeeStructureStatus.ACTIVE))
                .containsExactly(configured);

        verify(repository, never()).findAllConfiguredAssessmentsWithCustomCategory(
                1L, 2L, academicYears, StudentCategory.VJNT, null,
                "MALE", "First Year", FeeStructureStatus.ACTIVE);
    }

    @Test
    void otherCategoryUsesTheCaseInsensitiveCustomCategoryQuery() {
        FeeStructure configured = new FeeStructure();
        List<String> academicYears = List.of("2026-2027", "2026-27");
        when(repository.findAllConfiguredAssessmentsWithCustomCategory(
                1L, 2L, academicYears, StudentCategory.OTHER, "SEBC",
                "FEMALE", "First Year", FeeStructureStatus.ACTIVE))
                .thenReturn(List.of(configured));

        assertThat(repository.findConfiguredAssessments(
                1L, 2L, academicYears, StudentCategory.OTHER, "SEBC",
                "FEMALE", "First Year", FeeStructureStatus.ACTIVE))
                .containsExactly(configured);

        verify(repository, never()).findAllConfiguredAssessmentsWithoutCustomCategory(
                1L, 2L, academicYears, StudentCategory.OTHER,
                "FEMALE", "First Year", FeeStructureStatus.ACTIVE);
    }
}
