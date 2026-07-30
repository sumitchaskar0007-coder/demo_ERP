package com.jadhavr.erp.concurrency;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.fee.entity.FeePayment;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.entity.FeeTransaction;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockingMappingTest {

    @Test
    void versionsOnlyTheTwoMutableAggregateRoots() throws Exception {
        assertVersionField(AdmissionForm.class);
        assertVersionField(FeeStructure.class);

        assertNoDeclaredVersion(BaseAuditEntity.class);
        assertNoDeclaredVersion(AdmissionStatusHistory.class);
        assertNoDeclaredVersion(StudentFeeAccount.class);
        assertNoDeclaredVersion(FeePayment.class);
        assertNoDeclaredVersion(FeeTransaction.class);
    }

    @Test
    void migrationAddsOnlyTheTwoRequiredVersionColumns() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V24__optimistic_lock_mutable_workflows.sql")) {
            assertNotNull(stream);
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase();
        }

        assertTrue(migration.contains("alter table admission_forms"));
        assertTrue(migration.contains("alter table fee_structures"));
        assertEquals(2, migration.split("add column if not exists version", -1).length - 1);
        assertFalse(migration.contains("student_fee_accounts"));
        assertFalse(migration.contains("fee_payments"));
        assertFalse(migration.contains("fee_transactions"));
    }

    private static void assertVersionField(Class<?> entityType) throws Exception {
        Field field = entityType.getDeclaredField("version");
        assertEquals(long.class, field.getType());
        assertNotNull(field.getAnnotation(Version.class));

        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        assertFalse(column.nullable());
    }

    private static void assertNoDeclaredVersion(Class<?> entityType) {
        assertFalse(java.util.Arrays.stream(entityType.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(Version.class)));
    }
}
