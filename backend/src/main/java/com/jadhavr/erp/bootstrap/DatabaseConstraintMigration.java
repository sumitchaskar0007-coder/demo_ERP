package com.jadhavr.erp.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Keeps PostgreSQL enum check constraints in sync when Hibernate ddl-auto=update cannot alter them. */
@Component
public class DatabaseConstraintMigration implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public DatabaseConstraintMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        addStudentCategoryConstraints();
        addFinancialConstraints();

        jdbc.execute("ALTER TABLE admission_status_history DROP CONSTRAINT IF EXISTS admission_status_history_action_check");
        jdbc.execute("""
                ALTER TABLE admission_status_history
                ADD CONSTRAINT admission_status_history_action_check CHECK (action IN (
                    'SUBMITTED', 'STUDENT_SECTION_REVIEW_STARTED', 'STUDENT_SECTION_APPROVED',
                    'STUDENT_SECTION_REJECTED', 'ADMISSION_FORM_PRINTED', 'STATUS_UPDATED',
                    'FEE_ACCOUNT_CREATED', 'PAYMENT_SUBMITTED', 'PAYMENT_VERIFIED',
                    'PAYMENT_REJECTED', 'PRINCIPAL_REVIEW_PENDING'
                ))
                """);
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

    private void addCheckConstraintIfMissing(String table, String constraint, String condition) {
        jdbc.execute("DO $migration$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '"
                + constraint + "') THEN ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                + " CHECK (" + condition + "); END IF; END $migration$;");
    }
}
