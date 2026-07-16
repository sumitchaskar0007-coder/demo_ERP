CREATE INDEX IF NOT EXISTS idx_students_college_department_status
    ON student_profiles (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_students_user
    ON student_profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_admissions_college_department_status
    ON admission_forms (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_admissions_student_created
    ON admission_forms (student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_staff_college_department_status
    ON staff_profiles (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_fee_accounts_college_department_status
    ON student_fee_accounts (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_fee_accounts_student_created
    ON student_fee_accounts (student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fee_payments_college_status_created
    ON fee_payments (college_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_attendance_records_student
    ON attendance_records (student_id, status);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_section_date
    ON attendance_sessions (section_id, attendance_date DESC);
CREATE INDEX IF NOT EXISTS idx_sections_college_status
    ON course_year_divisions (college_id, status);
CREATE INDEX IF NOT EXISTS idx_subjects_college_department_status
    ON course_year_subjects (college_id, department_id, status);
CREATE INDEX IF NOT EXISTS idx_notices_created
    ON notices (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notices_creator_created
    ON notices (created_by_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notice_colleges_college_notice
    ON notice_colleges (college_id, notice_id);
CREATE INDEX IF NOT EXISTS idx_notice_roles_role_notice
    ON notice_audience_roles (role_name, notice_id);
