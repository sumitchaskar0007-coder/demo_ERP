-- Forward-only replacement for the former DatabaseConstraintMigration startup runner.
-- This migration intentionally fails when existing financial data violates the
-- constraints so incompatible data is not silently changed during deployment.

DO $migration$
DECLARE
    source_table text;
    target_table text;
    constraint_name text;
BEGIN
    FOR source_table, target_table IN
        SELECT * FROM (VALUES
            ('student_section_enrollments', 'academic_classes'),
            ('student_section_enrollments', 'academic_sections'),
            ('subject_teacher_assignments', 'academic_subjects')
        ) AS legacy(source_table, target_table)
    LOOP
        IF to_regclass(source_table) IS NOT NULL AND to_regclass(target_table) IS NOT NULL THEN
            FOR constraint_name IN
                SELECT c.conname
                FROM pg_constraint c
                WHERE c.contype = 'f'
                  AND c.conrelid = to_regclass(source_table)
                  AND c.confrelid = to_regclass(target_table)
            LOOP
                EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', source_table, constraint_name);
            END LOOP;
        END IF;
    END LOOP;
END $migration$;

ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;
ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (name IN (
    'SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'HOD', 'STUDENT_SECTION',
    'FEE_SECTION', 'CLASS_TEACHER', 'SUBJECT_TEACHER', 'GENERAL_STAFF', 'STUDENT'
));

ALTER TABLE staff_profiles DROP CONSTRAINT IF EXISTS staff_profiles_staff_type_check;
ALTER TABLE staff_profiles ADD CONSTRAINT staff_profiles_staff_type_check CHECK (staff_type IN (
    'STUDENT_SECTION', 'FEE_SECTION', 'HOD', 'TEACHER',
    'CLASS_TEACHER', 'SUBJECT_TEACHER', 'GENERAL_STAFF'
));

UPDATE admission_forms
SET status = 'SUBMITTED'
WHERE status = 'STUDENT_DETAILS_PENDING';

UPDATE admission_status_history
SET old_status = 'SUBMITTED'
WHERE old_status = 'STUDENT_DETAILS_PENDING';

UPDATE admission_status_history
SET new_status = 'SUBMITTED'
WHERE new_status = 'STUDENT_DETAILS_PENDING';

UPDATE admission_status_history
SET action = 'SUBMITTED'
WHERE action = 'STUDENT_DETAILS_SUBMITTED';

ALTER TABLE admission_forms DROP CONSTRAINT IF EXISTS admission_forms_status_check;
ALTER TABLE admission_forms ADD CONSTRAINT admission_forms_status_check CHECK (status IN (
    'SUBMITTED', 'STUDENT_SECTION_REVIEW_PENDING', 'STUDENT_SECTION_APPROVED',
    'STUDENT_SECTION_REJECTED', 'PRINCIPAL_REVIEW_PENDING', 'PRINCIPAL_APPROVED',
    'PRINCIPAL_REJECTED', 'CANCELLED'
));

ALTER TABLE admission_status_history DROP CONSTRAINT IF EXISTS admission_status_history_old_status_check;
ALTER TABLE admission_status_history ADD CONSTRAINT admission_status_history_old_status_check CHECK (old_status IN (
    'SUBMITTED', 'STUDENT_SECTION_REVIEW_PENDING', 'STUDENT_SECTION_APPROVED',
    'STUDENT_SECTION_REJECTED', 'PRINCIPAL_REVIEW_PENDING', 'PRINCIPAL_APPROVED',
    'PRINCIPAL_REJECTED', 'CANCELLED'
));

ALTER TABLE admission_status_history DROP CONSTRAINT IF EXISTS admission_status_history_new_status_check;
ALTER TABLE admission_status_history ADD CONSTRAINT admission_status_history_new_status_check CHECK (new_status IN (
    'SUBMITTED', 'STUDENT_SECTION_REVIEW_PENDING', 'STUDENT_SECTION_APPROVED',
    'STUDENT_SECTION_REJECTED', 'PRINCIPAL_REVIEW_PENDING', 'PRINCIPAL_APPROVED',
    'PRINCIPAL_REJECTED', 'CANCELLED'
));

INSERT INTO roles (name, description, created_at, updated_at)
VALUES
    ('SUPER_ADMIN', 'SUPER ADMIN role', now(), now()),
    ('ADMIN', 'ADMIN role', now(), now()),
    ('PRINCIPAL', 'PRINCIPAL role', now(), now()),
    ('HOD', 'HOD role', now(), now()),
    ('STUDENT_SECTION', 'STUDENT SECTION role', now(), now()),
    ('FEE_SECTION', 'FEE SECTION role', now(), now()),
    ('CLASS_TEACHER', 'CLASS TEACHER role', now(), now()),
    ('SUBJECT_TEACHER', 'SUBJECT TEACHER role', now(), now()),
    ('GENERAL_STAFF', 'GENERAL STAFF role', now(), now()),
    ('STUDENT', 'STUDENT role', now(), now())
ON CONFLICT (name) DO NOTHING;

UPDATE admission_forms SET student_category = 'OPEN' WHERE student_category IS NULL;
UPDATE student_profiles SET student_category = 'OPEN' WHERE student_category IS NULL;
UPDATE student_fee_accounts SET student_category = 'OPEN' WHERE student_category IS NULL;

ALTER TABLE admission_forms ALTER COLUMN student_category SET DEFAULT 'OPEN';
ALTER TABLE admission_forms ALTER COLUMN student_category SET NOT NULL;
ALTER TABLE admission_forms DROP CONSTRAINT IF EXISTS admission_forms_student_category_check;
ALTER TABLE admission_forms ADD CONSTRAINT admission_forms_student_category_check
    CHECK (student_category IN ('OPEN', 'OBC', 'SC', 'ST', 'SBC', 'VJNT', 'EWS', 'OTHER'));

ALTER TABLE student_profiles ALTER COLUMN student_category SET DEFAULT 'OPEN';
ALTER TABLE student_profiles ALTER COLUMN student_category SET NOT NULL;
ALTER TABLE student_profiles DROP CONSTRAINT IF EXISTS student_profiles_student_category_check;
ALTER TABLE student_profiles ADD CONSTRAINT student_profiles_student_category_check
    CHECK (student_category IN ('OPEN', 'OBC', 'SC', 'ST', 'SBC', 'VJNT', 'EWS', 'OTHER'));

ALTER TABLE student_fee_accounts ALTER COLUMN student_category SET DEFAULT 'OPEN';
ALTER TABLE student_fee_accounts ALTER COLUMN student_category SET NOT NULL;
ALTER TABLE student_fee_accounts DROP CONSTRAINT IF EXISTS student_fee_accounts_student_category_check;
ALTER TABLE student_fee_accounts ADD CONSTRAINT student_fee_accounts_student_category_check
    CHECK (student_category IN ('OPEN', 'OBC', 'SC', 'ST', 'SBC', 'VJNT', 'EWS', 'OTHER'));

ALTER TABLE student_fee_accounts DROP CONSTRAINT IF EXISTS student_fee_accounts_balance_check;
ALTER TABLE student_fee_accounts ADD CONSTRAINT student_fee_accounts_balance_check CHECK (
    total_fee >= 0 AND paid_amount >= 0 AND remaining_amount >= 0
    AND discount_amount >= 0 AND minimum_amount_for_admission >= 0
    AND paid_amount <= total_fee AND minimum_amount_for_admission <= total_fee
);

ALTER TABLE fee_payments DROP CONSTRAINT IF EXISTS fee_payments_amount_check;
ALTER TABLE fee_payments ADD CONSTRAINT fee_payments_amount_check CHECK (amount > 0);

ALTER TABLE admission_status_history DROP CONSTRAINT IF EXISTS admission_status_history_action_check;
ALTER TABLE admission_status_history ADD CONSTRAINT admission_status_history_action_check CHECK (action IN (
    'SUBMITTED', 'STUDENT_SECTION_REVIEW_STARTED', 'STUDENT_SECTION_APPROVED',
    'STUDENT_SECTION_REJECTED', 'ADMISSION_FORM_PRINTED', 'STATUS_UPDATED',
    'FEE_ACCOUNT_CREATED', 'PAYMENT_SUBMITTED', 'PAYMENT_VERIFIED',
    'PAYMENT_REJECTED', 'PRINCIPAL_REVIEW_PENDING', 'PRINCIPAL_APPROVED',
    'PRINCIPAL_REJECTED'
));

CREATE UNIQUE INDEX IF NOT EXISTS uk_fee_transaction_payment
    ON fee_transactions (fee_payment_id) WHERE fee_payment_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_college_reference_ci
    ON fee_payments (college_id, lower(transaction_reference));
CREATE INDEX IF NOT EXISTS idx_admission_scope_status
    ON admission_forms (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_student_scope_status
    ON student_profiles (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_staff_scope_status
    ON staff_profiles (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_fee_account_scope_status
    ON student_fee_accounts (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_fee_account_pending
    ON student_fee_accounts (college_id, remaining_amount DESC) WHERE remaining_amount > 0;
CREATE INDEX IF NOT EXISTS idx_payment_verified
    ON fee_payments (college_id, payment_date DESC) WHERE status = 'VERIFIED';
CREATE INDEX IF NOT EXISTS idx_enrollment_scope
    ON student_section_enrollments (academic_class_id, section_id, status, student_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_student_status
    ON student_section_enrollments (student_id, status);
CREATE INDEX IF NOT EXISTS idx_attendance_session_scope
    ON weekly_attendance_sessions (college_id, attendance_date, section_id);
CREATE INDEX IF NOT EXISTS idx_attendance_record_student
    ON weekly_attendance_records (student_id, session_id, status);
CREATE INDEX IF NOT EXISTS idx_timetable_teacher_slot
    ON weekly_timetable_entries (teacher_id, day_of_week, period_id);
CREATE INDEX IF NOT EXISTS idx_notice_college ON notice_colleges (college_id, notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_audience ON notice_audience_roles (role_name, notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_active
    ON notices (created_at DESC) WHERE deleted_at IS NULL;
