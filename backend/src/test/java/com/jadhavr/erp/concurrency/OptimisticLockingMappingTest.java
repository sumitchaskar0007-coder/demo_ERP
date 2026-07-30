package com.jadhavr.erp.concurrency;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.entity.AdmissionStatusHistory;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.fee.entity.FeePayment;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.fee.entity.FeeTransaction;
import com.jadhavr.erp.fee.entity.StudentFeeAccount;
import com.jadhavr.erp.timetable.entity.WeeklyTimetable;
import com.jadhavr.erp.user.entity.User;
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
    void versionsAllMutableAggregateRoots() throws Exception {
        assertVersionField(AdmissionForm.class);
        assertVersionField(FeeStructure.class);
        assertVersionField(StudentFeeAccount.class);
        assertVersionField(FeePayment.class);
        assertVersionField(WeeklyTimetable.class);
        assertVersionField(User.class);

        assertNoDeclaredVersion(BaseAuditEntity.class);
        assertNoDeclaredVersion(AdmissionStatusHistory.class);
        assertNoDeclaredVersion(FeeTransaction.class);
    }

    @Test
    void migrationsAddAllRequiredVersionColumns() throws IOException {
        String coreMigration = readMigration(
                "/db/migration/V27__optimistic_locking_and_hot_query_indexes.sql");
        String workflowMigration = readMigration(
                "/db/migration/V30__optimistic_lock_mutable_workflows.sql");

        assertTrue(coreMigration.contains("alter table users"));
        assertTrue(coreMigration.contains("alter table admission_forms"));
        assertTrue(coreMigration.contains("alter table student_fee_accounts"));
        assertTrue(coreMigration.contains("alter table fee_payments"));
        assertTrue(coreMigration.contains("alter table weekly_timetables"));
        assertTrue(workflowMigration.contains("alter table fee_structures"));
        assertFalse(coreMigration.contains("fee_transactions"));
        assertFalse(workflowMigration.contains("fee_transactions"));
    }

    private String readMigration(String path) throws IOException {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }
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
