-- Additive indexes for tenant-scoped list/report queries. These do not alter
-- data, constraints, ownership, or the externally managed RDS configuration.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_college_created_desc
    ON audit_logs (college_id, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_enrollment_section_status_student
    ON student_section_enrollments (section_id, status, student_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_subject_college_status_class
    ON course_year_subjects (college_id, status, academic_class_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_section_college_department_status_class
    ON course_year_divisions (college_id, department_id, status, academic_class_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_subject_teacher_subject_status_teacher
    ON subject_teacher_assignments (subject_id, status, teacher_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_subject_teacher_teacher_status
    ON subject_teacher_assignments (teacher_id, status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_staff_department_membership
    ON staff_profile_departments (department_id, staff_profile_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_weekly_attendance_college_date_status
    ON weekly_attendance_sessions (college_id, attendance_date, status);
