package com.collegeerp.erp.fee.repository;

import com.collegeerp.erp.fee.entity.FeeStructure;
import com.collegeerp.erp.fee.enums.FeeStructureStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_REPOSITORY_TESTS", matches = "true")
class FeeStructureRepositoryPostgresTest {
    @Autowired FeeStructureRepository repository;

    @Test
    void standardCategoryWithNullCustomNameIsResolvedByPostgres() {
        FeeStructure configured = repository.findAll().stream()
                .filter(structure -> structure.getStatus() == FeeStructureStatus.ACTIVE)
                .filter(structure -> structure.getGender() != null)
                .filter(structure -> structure.getCourseYear() != null)
                .filter(structure -> structure.getCustomCategoryName() == null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "A standard active fee structure is required for this integration test"));

        assertThat(repository.findConfiguredAssessments(
                configured.getCollege().getId(),
                configured.getDepartment().getId(),
                List.of(configured.getAcademicYear()),
                configured.getStudentCategory(),
                null,
                configured.getGender(),
                configured.getCourseYear(),
                FeeStructureStatus.ACTIVE))
                .extracting(FeeStructure::getId)
                .containsExactly(configured.getId());
    }
}
