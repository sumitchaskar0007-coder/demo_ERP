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
}
