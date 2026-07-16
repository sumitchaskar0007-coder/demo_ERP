-- ============================================================
-- CLEANUP SCRIPT: Remove all data except admin login
-- Database: PostgreSQL (college_erp)
--
-- What this script does:
--   1. Disables foreign key constraints
--   2. Truncates ALL tables (data + sequences)
--   3. Re-enables foreign key constraints
--   4. Re-seeds roles and admin user
--
-- Usage:
--   psql -U postgres -d college_erp -f cleanup_all_except_admin.sql
--
-- After running, restart the Spring Boot app.
-- DataSeeder.java will verify roles + admin exist (idempotent).
-- ============================================================

-- Step 1: Disable all foreign key triggers (safe truncate)
SET session_replication_role = 'replica';

-- Step 2: Truncate ALL tables (order doesn't matter with triggers disabled)
-- Tables containing non-admin data to clear:
TRUNCATE TABLE
    -- Academic (new managed)
    academic_classes,
    academic_holidays,
    academic_periods,
    academic_programs,
    academic_rooms,
    academic_sections,
    academic_semesters,
    academic_subjects,
    academic_terms,
    academic_working_days,
    academic_years,

    -- Academic (legacy)
    course_years,
    course_year_subjects,
    course_year_divisions,

    -- Admission
    admission_forms,
    admission_academic_records,
    admission_status_history,

    -- Attendance
    attendance_sessions,
    attendance_records,
    attendance_corrections,
    student_attendance,

    -- Audit
    audit_logs,
    erp_audit_logs,

    -- Auth
    refresh_tokens,
    security_audit_events,

    -- College / Department
    colleges,
    departments,

    -- Email
    email_notifications,
    email_verification_tokens,
    password_reset_tokens,

    -- Fee
    fee_payments,
    fee_structures,
    fee_transactions,
    student_fee_accounts,

    -- Notice
    notices,
    notice_colleges,
    notice_audience_roles,

    -- Staff / Student
    staff_profiles,
    student_profiles,
    student_enrollments,
    student_section_enrollments,

    -- Timetable
    timetables,
    timetable_entries,
    weekly_timetables,
    weekly_timetable_periods,
    weekly_timetable_entries,

    -- Assignments
    subject_teacher_assignments,
    teacher_subject_assignments,
    class_teacher_assignments
CASCADE;

-- Step 3: Re-enable foreign key triggers
SET session_replication_role = 'origin';

-- Step 4: Remove ALL users except the Super Admin
-- Keep only the user seeded by DataSeeder (email: admin@erp.com)
DELETE FROM user_roles
WHERE user_id != (SELECT id FROM users WHERE email = 'admin@erp.com');

DELETE FROM users
WHERE email != 'admin@erp.com';

-- Step 5: Reset sequences for tables with serial/bigserial IDs
-- (Hibernate will handle this on next startup, but explicit is better)
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS roles_id_seq RESTART WITH 1;

-- Done! Restart the Spring Boot application.
-- DataSeeder will confirm roles + admin are present.
