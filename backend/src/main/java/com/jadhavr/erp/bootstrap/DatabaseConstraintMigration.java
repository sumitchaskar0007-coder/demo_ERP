package com.jadhavr.erp.bootstrap;

import com.jadhavr.erp.admission.enums.AdmissionAction;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.user.entity.RoleName;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Keeps PostgreSQL enum check constraints in sync when Hibernate ddl-auto=update cannot alter them. */
@Component
@Order(0)
public class DatabaseConstraintMigration implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public DatabaseConstraintMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        removeLegacyAcademicForeignKeys();
        migrateLegacyAdmissionValues();
        synchronizeApplicationEnumConstraints();
        addStudentCategoryConstraints();
        addFinancialConstraints();
        addPerformanceIndexes();
    }

    private void migrateLegacyAdmissionValues() {
        jdbc.update("UPDATE admission_forms SET status = 'SUBMITTED' WHERE status = 'STUDENT_DETAILS_PENDING'");
        jdbc.update("UPDATE admission_status_history SET old_status = 'SUBMITTED' WHERE old_status = 'STUDENT_DETAILS_PENDING'");
        jdbc.update("UPDATE admission_status_history SET new_status = 'SUBMITTED' WHERE new_status = 'STUDENT_DETAILS_PENDING'");
        jdbc.update("UPDATE admission_status_history SET action = 'SUBMITTED' WHERE action = 'STUDENT_DETAILS_SUBMITTED'");
    }

    /**
     * Course-year entities were moved out of the legacy academic master tables.
     * Hibernate adds the new foreign keys but cannot remove the obsolete ones,
     * leaving inserts required to exist in both unrelated tables.
     */
    private void removeLegacyAcademicForeignKeys() {
        dropForeignKeysReferencing("student_section_enrollments", "academic_classes");
        dropForeignKeysReferencing("student_section_enrollments", "academic_sections");
        dropForeignKeysReferencing("subject_teacher_assignments", "academic_subjects");
    }

    private void dropForeignKeysReferencing(String sourceTable, String legacyTargetTable) {
        jdbc.execute("""
                DO $migration$
                DECLARE constraint_name text;
                BEGIN
                  FOR constraint_name IN
                    SELECT c.conname
                    FROM pg_constraint c
                    WHERE c.contype = 'f'
                      AND c.conrelid = '%s'::regclass
                      AND c.confrelid = '%s'::regclass
                  LOOP
                    EXECUTE format('ALTER TABLE %%I DROP CONSTRAINT %%I', '%s', constraint_name);
                  END LOOP;
                END $migration$;
                """.formatted(sourceTable, legacyTargetTable, sourceTable));
    }

    private void synchronizeApplicationEnumConstraints() {
        replaceCheckConstraint("roles", "roles_name_check",
                enumCondition("name", RoleName.values()));
        replaceCheckConstraint("staff_profiles", "staff_profiles_staff_type_check",
                enumCondition("staff_type", StaffType.values()));
        replaceCheckConstraint("admission_forms", "admission_forms_status_check",
                enumCondition("status", AdmissionStatus.values()));
        replaceCheckConstraint("admission_status_history", "admission_status_history_old_status_check",
                enumCondition("old_status", AdmissionStatus.values()));
        replaceCheckConstraint("admission_status_history", "admission_status_history_new_status_check",
                enumCondition("new_status", AdmissionStatus.values()));
        replaceCheckConstraint("admission_status_history", "admission_status_history_action_check",
                enumCondition("action", AdmissionAction.values()));
    }

    private String enumCondition(String column, Enum<?>[] values) {
        return column + " IN (" + java.util.Arrays.stream(values)
                .map(value -> "'" + value.name() + "'")
                .collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    private void replaceCheckConstraint(String table, String constraint, String condition) {
        jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        jdbc.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                + " CHECK (" + condition + ")");
    }

    private void addStudentCategoryConstraints() {
        for (String table : new String[]{"admission_forms", "student_profiles", "student_fee_accounts"}) {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN student_category SET DEFAULT 'OPEN'");
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN student_category SET NOT NULL");
            addCheckConstraintIfMissing(table, table + "_student_category_check",
                    "student_category IN ('OPEN','OBC','SC','ST','SBC','VJNT','EWS','OTHER')");
        }
    }

    private void addFinancialConstraints() {
        addCheckConstraintIfMissing("student_fee_accounts", "student_fee_accounts_balance_check",
                "total_fee >= 0 AND paid_amount >= 0 AND remaining_amount >= 0 "
                        + "AND discount_amount >= 0 AND minimum_amount_for_admission >= 0 "
                        + "AND paid_amount <= total_fee AND minimum_amount_for_admission <= total_fee");
        addCheckConstraintIfMissing("fee_payments", "fee_payments_amount_check", "amount > 0");
        jdbc.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_fee_transaction_payment
                ON fee_transactions (fee_payment_id)
                WHERE fee_payment_id IS NOT NULL
                """);
        jdbc.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_college_reference_ci
                ON fee_payments (college_id, lower(transaction_reference))
                """);
    }

    private void addPerformanceIndexes() {
        String[] statements = {
                "CREATE INDEX IF NOT EXISTS idx_admission_scope_status ON admission_forms (college_id, department_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_student_scope_status ON student_profiles (college_id, department_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_staff_scope_status ON staff_profiles (college_id, department_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_fee_account_scope_status ON student_fee_accounts (college_id, department_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_fee_account_pending ON student_fee_accounts (college_id, remaining_amount DESC) WHERE remaining_amount > 0",
                "CREATE INDEX IF NOT EXISTS idx_payment_verified ON fee_payments (college_id, payment_date DESC) WHERE status = 'VERIFIED'",
                "CREATE INDEX IF NOT EXISTS idx_enrollment_scope ON student_section_enrollments (academic_class_id, section_id, status, student_id)",
                "CREATE INDEX IF NOT EXISTS idx_enrollment_student_status ON student_section_enrollments (student_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_attendance_session_scope ON weekly_attendance_sessions (college_id, attendance_date, section_id)",
                "CREATE INDEX IF NOT EXISTS idx_attendance_record_student ON weekly_attendance_records (student_id, session_id, status)",
                "CREATE INDEX IF NOT EXISTS idx_timetable_teacher_slot ON weekly_timetable_entries (teacher_id, day_of_week, period_id)",
                "CREATE INDEX IF NOT EXISTS idx_notice_college ON notice_colleges (college_id, notice_id)",
                "CREATE INDEX IF NOT EXISTS idx_notice_audience ON notice_audience_roles (role_name, notice_id)",
                "CREATE INDEX IF NOT EXISTS idx_notice_active ON notices (created_at DESC) WHERE deleted_at IS NULL"
        };
        for (String statement : statements) jdbc.execute(statement);
    }

    private void addCheckConstraintIfMissing(String table, String constraint, String condition) {
        jdbc.execute("DO $migration$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '"
                + constraint + "') THEN ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                + " CHECK (" + condition + "); END IF; END $migration$;");
    }
}
