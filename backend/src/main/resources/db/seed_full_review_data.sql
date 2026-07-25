\set ON_ERROR_STOP on

BEGIN;

CREATE TEMP TABLE preserved_super_admin AS
SELECT * FROM users WHERE email = 'admin@erp.com' ORDER BY id LIMIT 1;

DO $$
DECLARE
    table_list text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ' ORDER BY tablename)
    INTO table_list
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename <> 'flyway_schema_history';

    IF table_list IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || table_list || ' RESTART IDENTITY CASCADE';
    END IF;
END $$;

INSERT INTO roles (id, created_at, updated_at, name, description) VALUES
    (1, now(), now(), 'SUPER_ADMIN', 'Platform super administrator'),
    (2, now(), now(), 'PRINCIPAL', 'College principal'),
    (3, now(), now(), 'HOD', 'Head of department'),
    (4, now(), now(), 'STUDENT_SECTION', 'Student section operator'),
    (5, now(), now(), 'FEE_SECTION', 'Fee section operator'),
    (6, now(), now(), 'CLASS_TEACHER', 'Class teacher'),
    (7, now(), now(), 'SUBJECT_TEACHER', 'Subject teacher'),
    (8, now(), now(), 'STUDENT', 'Student'),
    (9, now(), now(), 'ADMIN', 'College administrator'),
    (10, now(), now(), 'GENERAL_STAFF', 'General staff');

INSERT INTO users (
    id, created_at, updated_at, email, full_name, password_hash, phone, status,
    address, bio, profile_image_url, failed_login_attempts, session_version,
    email_verified, must_change_password
)
SELECT
    1, now(), now(), 'admin@erp.com', COALESCE(full_name, 'Super Admin'), password_hash,
    COALESCE(phone, '9999999999'), 'ACTIVE', address, bio, profile_image_url,
    0, 0, true, false
FROM preserved_super_admin;

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

SELECT setval(pg_get_serial_sequence('roles', 'id'), 10, true);
SELECT setval(pg_get_serial_sequence('users', 'id'), 1, true);

INSERT INTO colleges (
    created_at, updated_at, code, name, address, city, state, pincode,
    contact_email, contact_phone, status
) VALUES (
    now(), now(), 'DEMO', 'Jadhavar Institute of Management and Technology',
    'Narhe-Dhayari Road, Narhe', 'Pune', 'Maharashtra', '411041',
    'office@demo.erp', '9999989999', 'ACTIVE'
);

INSERT INTO departments (created_at, updated_at, college_id, code, name, description, status)
SELECT c.created_at, c.updated_at, c.id, v.code, v.name, v.description, 'ACTIVE'
FROM colleges c
CROSS JOIN (VALUES
    ('CSE', 'Computer Science and Engineering', 'Computing, software and data studies'),
    ('BBA', 'Business Administration', 'Management, finance and entrepreneurship studies')
) AS v(code, name, description)
WHERE c.code = 'DEMO';

INSERT INTO users (
    created_at, updated_at, email, full_name, password_hash, phone, status,
    college_id, address, failed_login_attempts, session_version, email_verified,
    must_change_password
)
SELECT
    now(), now(), v.email, v.full_name, admin.password_hash, v.phone, 'ACTIVE', c.id,
    'Pune, Maharashtra', 0, 0, true, false
FROM colleges c
JOIN users admin ON admin.email = 'admin@erp.com'
CROSS JOIN (VALUES
    ('principal@demo.erp', 'Dr. Anjali Jadhavar', '9000000001'),
    ('hod.cse@demo.erp', 'Prof. Rahul Kulkarni', '9000000002'),
    ('hod.bba@demo.erp', 'Prof. Meera Deshmukh', '9000000003'),
    ('studentsection@demo.erp', 'Sneha Student Section', '9000000004'),
    ('feesection@demo.erp', 'Rohan Fee Section', '9000000005'),
    ('staff@demo.erp', 'Kavita General Staff', '9000000006'),
    ('ct.cse1@demo.erp', 'Amit CSE Class Teacher I', '9000000011'),
    ('ct.cse2@demo.erp', 'Neha CSE Class Teacher II', '9000000012'),
    ('ct.cse3@demo.erp', 'Vikas CSE Class Teacher III', '9000000013'),
    ('ct.bba1@demo.erp', 'Pooja BBA Class Teacher I', '9000000021'),
    ('ct.bba2@demo.erp', 'Sameer BBA Class Teacher II', '9000000022'),
    ('ct.bba3@demo.erp', 'Ritu BBA Class Teacher III', '9000000023'),
    ('teacher.cse1@demo.erp', 'Arjun Patil', '9000000031'),
    ('teacher.cse2@demo.erp', 'Priya Joshi', '9000000032'),
    ('teacher.cse3@demo.erp', 'Nitin Shinde', '9000000033'),
    ('teacher.bba1@demo.erp', 'Sonal Shah', '9000000041'),
    ('teacher.bba2@demo.erp', 'Akash Gupta', '9000000042'),
    ('teacher.bba3@demo.erp', 'Manasi Rao', '9000000043')
) AS v(email, full_name, phone)
WHERE c.code = 'DEMO';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = CASE
    WHEN u.email = 'principal@demo.erp' THEN 'PRINCIPAL'
    WHEN u.email LIKE 'hod.%' THEN 'HOD'
    WHEN u.email = 'studentsection@demo.erp' THEN 'STUDENT_SECTION'
    WHEN u.email = 'feesection@demo.erp' THEN 'FEE_SECTION'
    WHEN u.email = 'staff@demo.erp' THEN 'GENERAL_STAFF'
    WHEN u.email LIKE 'ct.%' THEN 'CLASS_TEACHER'
    WHEN u.email LIKE 'teacher.%' THEN 'SUBJECT_TEACHER'
END
WHERE u.email LIKE '%@demo.erp';

INSERT INTO staff_profiles (
    created_at, updated_at, email, employee_code, full_name, joining_date, phone,
    staff_type, status, college_id, department_id, user_id
)
SELECT
    now(), now(), u.email,
    'EMP-' || lpad(row_number() OVER (ORDER BY u.id)::text, 3, '0'),
    u.full_name, DATE '2024-06-15', u.phone,
    CASE
        WHEN u.email LIKE 'hod.%' THEN 'HOD'
        WHEN u.email = 'studentsection@demo.erp' THEN 'STUDENT_SECTION'
        WHEN u.email = 'feesection@demo.erp' THEN 'FEE_SECTION'
        WHEN u.email = 'staff@demo.erp' THEN 'GENERAL_STAFF'
        WHEN u.email LIKE 'ct.%' THEN 'CLASS_TEACHER'
        ELSE 'SUBJECT_TEACHER'
    END,
    'ACTIVE', u.college_id, d.id, u.id
FROM users u
LEFT JOIN departments d
  ON d.college_id = u.college_id
 AND d.code = CASE
    WHEN u.email LIKE '%.cse%' THEN 'CSE'
    WHEN u.email LIKE '%.bba%' THEN 'BBA'
 END
WHERE u.email LIKE '%@demo.erp'
  AND u.email <> 'principal@demo.erp';

INSERT INTO staff_profile_departments (staff_profile_id, department_id)
SELECT sp.id, d.id
FROM staff_profiles sp
JOIN departments d ON d.college_id = sp.college_id
WHERE sp.department_id = d.id OR sp.department_id IS NULL;

INSERT INTO academic_years (created_at, updated_at, active, start_date, end_date, name, college_id)
SELECT now(), now(), true, DATE '2026-06-15', DATE '2027-05-31', '2026-2027', id
FROM colleges WHERE code = 'DEMO';

INSERT INTO academic_programs (created_at, updated_at, code, name, college_id)
SELECT now(), now(), d.code, d.name, d.college_id
FROM departments d;

INSERT INTO course_years (
    created_at, updated_at, academic_year, code, name, year_name, description,
    status, college_id, department_id
)
SELECT
    now(), now(), '2026-2027', d.code || '-' || v.short_code,
    v.display_name || ' - ' || d.code, v.year_name,
    v.display_name || ' curriculum for ' || d.name, 'ACTIVE', d.college_id, d.id
FROM departments d
CROSS JOIN (VALUES
    ('FY', 'First Year', 'FIRST_YEAR'),
    ('SY', 'Second Year', 'SECOND_YEAR'),
    ('TY', 'Third Year', 'THIRD_YEAR')
) AS v(short_code, display_name, year_name);

INSERT INTO academic_classes (
    created_at, updated_at, academic_year, code, name, year_name, description,
    status, college_id, department_id, program_id
)
SELECT
    now(), now(), cy.academic_year, cy.code, cy.name, cy.year_name, cy.description,
    'ACTIVE', cy.college_id, cy.department_id, p.id
FROM course_years cy
JOIN academic_programs p
  ON p.college_id = cy.college_id
 AND p.code = (SELECT code FROM departments WHERE id = cy.department_id);

INSERT INTO course_year_divisions (
    created_at, updated_at, academic_year, capacity, code, name, status,
    academic_class_id, college_id, department_id
)
SELECT now(), now(), academic_year, 60, code || '-A', 'Division A', 'ACTIVE', id, college_id, department_id
FROM course_years;

INSERT INTO academic_sections (
    created_at, updated_at, name, academic_year, capacity, code, status,
    college_id, class_id, academic_class_id, department_id
)
SELECT now(), now(), 'Division A', academic_year, 60, code || '-A', 'ACTIVE',
       college_id, id, id, department_id
FROM academic_classes;

UPDATE course_year_divisions div
SET class_teacher_id = sp.id
FROM course_years cy
JOIN departments d ON d.id = cy.department_id
JOIN staff_profiles sp ON sp.email = 'ct.' || lower(d.code) ||
    CASE cy.year_name WHEN 'FIRST_YEAR' THEN '1' WHEN 'SECOND_YEAR' THEN '2' ELSE '3' END || '@demo.erp'
WHERE div.academic_class_id = cy.id;

UPDATE academic_sections sec
SET class_teacher_id = sp.id
FROM academic_classes ac
JOIN departments d ON d.id = ac.department_id
JOIN staff_profiles sp ON sp.email = 'ct.' || lower(d.code) ||
    CASE ac.year_name WHEN 'FIRST_YEAR' THEN '1' WHEN 'SECOND_YEAR' THEN '2' ELSE '3' END || '@demo.erp'
WHERE sec.academic_class_id = ac.id;

INSERT INTO class_teacher_assignments (
    created_at, updated_at, college_id, class_id, academic_year_id, section_id, teacher_id
)
SELECT now(), now(), sec.college_id, ac.id, ay.id, sec.id, sp.user_id
FROM academic_sections sec
JOIN academic_classes ac ON ac.id = sec.academic_class_id
JOIN academic_years ay ON ay.college_id = sec.college_id AND ay.name = sec.academic_year
JOIN staff_profiles sp ON sp.id = sec.class_teacher_id;

INSERT INTO course_year_subjects (
    created_at, updated_at, academic_year, code, name, credits, description,
    status, subject_type, academic_class_id, college_id, department_id
)
SELECT
    now(), now(), cy.academic_year, cy.code || '-S' || v.n,
    CASE d.code
        WHEN 'CSE' THEN (ARRAY['Programming Fundamentals','Database Management','Computer Networks'])[v.n]
        ELSE (ARRAY['Principles of Management','Financial Accounting','Business Communication'])[v.n]
    END || ' ' || replace(initcap(lower(cy.year_name)), '_', ' '),
    CASE WHEN v.n = 3 THEN 2 ELSE 4 END, 'Demo curriculum subject', 'ACTIVE',
    CASE WHEN v.n = 3 THEN 'PRACTICAL' ELSE 'THEORY' END,
    cy.id, cy.college_id, cy.department_id
FROM course_years cy
JOIN departments d ON d.id = cy.department_id
CROSS JOIN generate_series(1, 3) AS v(n);

INSERT INTO academic_subjects (
    created_at, updated_at, code, name, type, academic_year, credits, description,
    status, college_id, program_id, academic_class_id, department_id
)
SELECT now(), now(), s.code, s.name,
       CASE WHEN s.subject_type = 'PRACTICAL' THEN 'LAB' ELSE 'THEORY' END,
       s.academic_year, s.credits, s.description, 'ACTIVE', s.college_id,
       ac.program_id, ac.id, s.department_id
FROM course_year_subjects s
JOIN course_years cy ON cy.id = s.academic_class_id
JOIN academic_classes ac
  ON ac.college_id = cy.college_id
 AND ac.department_id = cy.department_id
 AND ac.year_name = cy.year_name
 AND ac.academic_year = cy.academic_year;

WITH ranked_subjects AS (
    SELECT s.*, row_number() OVER (PARTITION BY s.department_id ORDER BY s.id) AS rn
    FROM course_year_subjects s
)
INSERT INTO subject_teacher_assignments (
    created_at, updated_at, academic_year, assigned_at, status, subject_id, teacher_id
)
SELECT now(), now(), s.academic_year, now(), 'ACTIVE', s.id, sp.id
FROM ranked_subjects s
JOIN departments d ON d.id = s.department_id
JOIN staff_profiles sp ON sp.email = 'teacher.' || lower(d.code) || (((s.rn - 1) % 3) + 1) || '@demo.erp';

INSERT INTO teacher_subject_assignments (
    created_at, updated_at, college_id, teacher_id, subject_id, class_id, section_id
)
SELECT now(), now(), s.college_id, sp.user_id, s.id, s.academic_class_id, sec.id
FROM academic_subjects s
JOIN academic_sections sec ON sec.academic_class_id = s.academic_class_id
JOIN course_year_subjects legacy ON legacy.code = s.code
JOIN subject_teacher_assignments sta ON sta.subject_id = legacy.id
JOIN staff_profiles sp ON sp.id = sta.teacher_id;

INSERT INTO academic_rooms (created_at, updated_at, capacity, code, name, type, college_id)
SELECT now(), now(), v.capacity, v.code, v.name, v.type, c.id
FROM colleges c
CROSS JOIN (VALUES
    (60, 'C101', 'Computer Classroom 101', 'CLASSROOM'),
    (40, 'C-LAB', 'Computer Laboratory', 'LAB'),
    (60, 'B101', 'Management Classroom 101', 'CLASSROOM'),
    (150, 'HALL', 'Seminar Hall', 'HALL')
) AS v(capacity, code, name, type);

INSERT INTO academic_working_days (created_at, updated_at, day_of_week, working, college_id)
SELECT now(), now(), d.day_name, d.day_no <= 6, c.id
FROM colleges c
CROSS JOIN (VALUES
    (1, 'MONDAY'), (2, 'TUESDAY'), (3, 'WEDNESDAY'), (4, 'THURSDAY'),
    (5, 'FRIDAY'), (6, 'SATURDAY'), (7, 'SUNDAY')
) AS d(day_no, day_name);

INSERT INTO academic_periods (created_at, updated_at, end_time, period_number, start_time, type, college_id)
SELECT now(), now(), v.end_time, v.position, v.start_time, v.kind, c.id
FROM colleges c
CROSS JOIN (VALUES
    (1, TIME '09:00', TIME '09:50', 'LECTURE'),
    (2, TIME '09:50', TIME '10:40', 'LECTURE'),
    (3, TIME '10:40', TIME '11:00', 'BREAK'),
    (4, TIME '11:00', TIME '11:50', 'LECTURE'),
    (5, TIME '11:50', TIME '12:40', 'LECTURE'),
    (6, TIME '12:40', TIME '13:20', 'BREAK'),
    (7, TIME '13:20', TIME '14:10', 'LECTURE'),
    (8, TIME '14:10', TIME '15:00', 'LECTURE')
) AS v(position, start_time, end_time, kind);

INSERT INTO weekly_timetables (created_at, updated_at, status, college_id, section_id)
SELECT now(), now(), 'ACTIVE', college_id, id FROM course_year_divisions;

INSERT INTO weekly_timetable_periods (
    created_at, updated_at, end_time, kind, label, position, start_time, timetable_id
)
SELECT now(), now(), v.end_time, v.kind, v.label, v.position, v.start_time, wt.id
FROM weekly_timetables wt
CROSS JOIN (VALUES
    (1, TIME '09:00', TIME '09:50', 'TEACHING', 'Lecture 1'),
    (2, TIME '09:50', TIME '10:40', 'TEACHING', 'Lecture 2'),
    (3, TIME '10:40', TIME '11:00', 'SHORT_BREAK', 'Short Break'),
    (4, TIME '11:00', TIME '11:50', 'TEACHING', 'Lecture 3'),
    (5, TIME '11:50', TIME '12:40', 'TEACHING', 'Lecture 4'),
    (6, TIME '12:40', TIME '13:20', 'LUNCH_BREAK', 'Lunch Break'),
    (7, TIME '13:20', TIME '14:10', 'TEACHING', 'Lecture 5'),
    (8, TIME '14:10', TIME '15:00', 'TEACHING', 'Lecture 6')
) AS v(position, start_time, end_time, kind, label);

WITH days(day_name, day_no) AS (VALUES
    ('MONDAY', 1), ('TUESDAY', 2), ('WEDNESDAY', 3),
    ('THURSDAY', 4), ('FRIDAY', 5), ('SATURDAY', 6)
)
INSERT INTO weekly_timetable_entries (
    created_at, updated_at, day_of_week, lecture_type, remarks, room,
    period_id, subject_id, teacher_id, timetable_id
)
SELECT
    now(), now(), days.day_name,
    CASE WHEN subject.subject_type = 'PRACTICAL' THEN 'LAB' ELSE 'THEORY' END,
    'Demo scheduled lecture', CASE WHEN d.code = 'CSE' THEN 'C101' ELSE 'B101' END,
    period.id, subject.id, assignment.teacher_id, wt.id
FROM weekly_timetables wt
JOIN course_year_divisions div ON div.id = wt.section_id
JOIN departments d ON d.id = div.department_id
JOIN weekly_timetable_periods period ON period.timetable_id = wt.id AND period.kind = 'TEACHING'
CROSS JOIN days
JOIN LATERAL (
    SELECT s.* FROM course_year_subjects s
    WHERE s.academic_class_id = div.academic_class_id
    ORDER BY s.id
    OFFSET ((days.day_no + period.position - 2) % 3) LIMIT 1
) subject ON true
JOIN subject_teacher_assignments assignment ON assignment.subject_id = subject.id;

INSERT INTO users (
    created_at, updated_at, email, full_name, password_hash, phone, status,
    college_id, address, failed_login_attempts, session_version, email_verified,
    must_change_password
)
SELECT
    now(), now(), 'student' || lpad(g.n::text, 3, '0') || '@demo.erp',
    'Demo Student ' || lpad(g.n::text, 3, '0'), admin.password_hash,
    '91' || lpad((70000000 + g.n)::text, 8, '0'), 'ACTIVE', c.id,
    'Pune, Maharashtra', 0, 0, true, false
FROM generate_series(1, 100) AS g(n)
CROSS JOIN colleges c
CROSS JOIN users admin
WHERE c.code = 'DEMO' AND admin.email = 'admin@erp.com';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email ~ '^student[0-9]{3}@demo\.erp$' AND r.name = 'STUDENT';

INSERT INTO student_profiles (
    created_at, updated_at, address_line1, city, date_of_birth, email,
    first_name, full_name, gender, last_name, parent_email, parent_name,
    parent_phone, phone, pincode, state, status, college_id, department_id,
    user_id, activated_at, admission_number, roll_number, student_category
)
SELECT
    now(), now(), 'Demo Address ' || g.n || ', Narhe', 'Pune',
    DATE '2007-01-01' + ((g.n % 365) * INTERVAL '1 day'), u.email,
    'Student', u.full_name, CASE WHEN g.n % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
    lpad(g.n::text, 3, '0'), 'parent' || g.n || '@demo.erp',
    'Parent of Student ' || g.n, '92' || lpad((70000000 + g.n)::text, 8, '0'),
    u.phone, '411041', 'Maharashtra', 'ACTIVE', u.college_id, d.id, u.id, now(),
    'ADM-2026-' || lpad(g.n::text, 3, '0'),
    d.code || '-26-' || lpad(g.n::text, 3, '0'),
    (ARRAY['OPEN','OBC','SC','ST','SBC','VJNT','EWS','OTHER'])[((g.n - 1) % 8) + 1]
FROM generate_series(1, 100) AS g(n)
JOIN users u ON u.email = 'student' || lpad(g.n::text, 3, '0') || '@demo.erp'
JOIN departments d ON d.college_id = u.college_id
 AND d.code = CASE WHEN g.n <= 50 THEN 'CSE' ELSE 'BBA' END;

INSERT INTO admission_forms (
    created_at, updated_at, academic_year, address_line1, city, date_of_birth,
    email, first_name, full_name, gender, last_name, parent_email, parent_name,
    parent_phone, phone, pincode, previous_class_name, previous_percentage,
    previous_school_name, principal_approved_at, print_count, source, state,
    status, student_section_verified_at, submitted_at, college_id, department_id,
    student_id, student_section_verified_by, student_user_id, aadhaar_number,
    apaar_id, caste, correspondence_address, correspondence_city,
    correspondence_email, correspondence_mobile, correspondence_pincode,
    correspondence_state, details_completed_at, marital_status, nationality,
    permanent_email, permanent_phone, place_of_birth, religion, student_category,
    admission_reference_number
)
SELECT
    now() - INTERVAL '30 days', now(), '2026-2027', sp.address_line1, sp.city,
    sp.date_of_birth, sp.email, sp.first_name, sp.full_name, sp.gender,
    sp.last_name, sp.parent_email, sp.parent_name, sp.parent_phone, sp.phone,
    sp.pincode, '12th Standard', 60 + (g.n % 35), 'Demo Junior College',
    now() - INTERVAL '20 days', 0, 'ADMIN_CREATED', sp.state,
    'PRINCIPAL_APPROVED', now() - INTERVAL '25 days', now() - INTERVAL '30 days',
    sp.college_id, sp.department_id, sp.id, verifier.id, sp.user_id,
    '500000' || lpad(g.n::text, 6, '0'), 'APAAR2026' || lpad(g.n::text, 3, '0'),
    CASE WHEN sp.student_category = 'OPEN' THEN 'General' ELSE sp.student_category END,
    sp.address_line1 || ', Pune', sp.city, sp.email, sp.phone, sp.pincode,
    sp.state, now() - INTERVAL '29 days', 'UNMARRIED', 'Indian', sp.email,
    sp.phone, 'Pune', 'Hindu', sp.student_category,
    'DEMO-ADM-2026-' || lpad(g.n::text, 3, '0')
FROM generate_series(1, 100) AS g(n)
JOIN student_profiles sp ON sp.admission_number = 'ADM-2026-' || lpad(g.n::text, 3, '0')
JOIN users verifier ON verifier.email = 'studentsection@demo.erp';

INSERT INTO admission_academic_records (
    admission_form_id, board_university, institute_name, marks_percentage,
    qualification, year_of_passing, record_order
)
SELECT af.id, 'Maharashtra State Board', 'Demo Junior College',
       af.previous_percentage, '12th', '2026', 1
FROM admission_forms af;

INSERT INTO admission_status_history (
    created_at, updated_at, action, new_status, old_status, remarks,
    admission_form_id, changed_by
)
SELECT now() - INTERVAL '20 days', now() - INTERVAL '20 days',
       'PRINCIPAL_APPROVED', 'PRINCIPAL_APPROVED', 'PRINCIPAL_REVIEW_PENDING',
       'Approved for complete functionality review', af.id, principal.id
FROM admission_forms af
JOIN users principal ON principal.email = 'principal@demo.erp';

WITH numbered_students AS (
    SELECT sp.*, substring(sp.admission_number FROM '[0-9]+$')::int AS n
    FROM student_profiles sp
)
INSERT INTO student_section_enrollments (
    created_at, updated_at, academic_year, enrolled_at, roll_number, status,
    academic_class_id, section_id, student_id
)
SELECT now(), now(), '2026-2027', now() - INTERVAL '15 days', sp.roll_number,
       'ACTIVE', cy.id, div.id, sp.id
FROM numbered_students sp
JOIN course_years cy ON cy.department_id = sp.department_id
 AND cy.year_name = (ARRAY['FIRST_YEAR','SECOND_YEAR','THIRD_YEAR'])[((sp.n - 1) % 3) + 1]
JOIN course_year_divisions div ON div.academic_class_id = cy.id;

INSERT INTO student_enrollments (
    created_at, updated_at, active, college_id, class_id, academic_year_id,
    section_id, student_id
)
SELECT now(), now(), true, sp.college_id, ac.id, ay.id, sec.id, sp.id
FROM student_profiles sp
JOIN student_section_enrollments legacy ON legacy.student_id = sp.id
JOIN course_years cy ON cy.id = legacy.academic_class_id
JOIN academic_classes ac ON ac.department_id = cy.department_id
 AND ac.year_name = cy.year_name AND ac.academic_year = cy.academic_year
JOIN academic_sections sec ON sec.academic_class_id = ac.id
JOIN academic_years ay ON ay.college_id = sp.college_id AND ay.name = cy.academic_year;

INSERT INTO fee_structures (
    created_at, updated_at, academic_year, admission_fee, description, exam_fee,
    library_fee, minimum_amount_for_admission, other_fee, status,
    student_category, title, total_fee, tuition_fee, college_id, department_id
)
SELECT
    now(), now(), '2026-2027', 5000, 'Complete demo fee structure', 3000,
    2000, 10000, 0, 'ACTIVE', category,
    d.code || ' 2026-27 ' || category || ' Fees',
    CASE d.code WHEN 'CSE' THEN 75000 ELSE 60000 END,
    CASE d.code WHEN 'CSE' THEN 65000 ELSE 50000 END,
    d.college_id, d.id
FROM departments d
CROSS JOIN unnest(ARRAY['OPEN','OBC','SC','ST','SBC','VJNT','EWS','OTHER']) AS category;

WITH fee_data AS (
    SELECT sp.*, af.id AS admission_id, fs.id AS structure_id, fs.total_fee,
           fs.minimum_amount_for_admission,
           substring(sp.admission_number FROM '[0-9]+$')::int AS n
    FROM student_profiles sp
    JOIN admission_forms af ON af.student_id = sp.id
    JOIN fee_structures fs ON fs.department_id = sp.department_id
      AND fs.academic_year = '2026-2027'
      AND fs.student_category = sp.student_category
)
INSERT INTO student_fee_accounts (
    created_at, updated_at, academic_year, discount_amount,
    minimum_amount_for_admission, paid_amount, remaining_amount, status,
    student_category, total_fee, admission_form_id, college_id, department_id,
    fee_structure_id, student_id, student_user_id
)
SELECT
    now(), now(), '2026-2027', 0, minimum_amount_for_admission,
    CASE n % 4 WHEN 0 THEN total_fee WHEN 1 THEN 25000 WHEN 2 THEN 0 ELSE 10000 END,
    total_fee - CASE n % 4 WHEN 0 THEN total_fee WHEN 1 THEN 25000 WHEN 2 THEN 0 ELSE 10000 END,
    CASE n % 4 WHEN 0 THEN 'PAID' WHEN 2 THEN 'PENDING' ELSE 'PARTIALLY_PAID' END,
    student_category, total_fee, admission_id, college_id, department_id,
    structure_id, id, user_id
FROM fee_data;

INSERT INTO fee_payments (
    created_at, updated_at, amount, payment_date, payment_mode, proof_url,
    remarks, status, submitted_at, transaction_reference, verified_at,
    college_id, department_id, student_id, fee_account_id, student_user_id,
    verified_by_id
)
SELECT
    now() - INTERVAL '10 days', now(), fa.paid_amount, DATE '2026-07-05',
    (ARRAY['UPI','BANK_TRANSFER','CASH','CHEQUE'])[((n - 1) % 4) + 1],
    'demo://payment-proof/' || n, 'Verified demo payment', 'VERIFIED',
    now() - INTERVAL '10 days', 'DEMO-PAY-' || lpad(n::text, 3, '0'),
    now() - INTERVAL '9 days', fa.college_id, fa.department_id, fa.student_id,
    fa.id, fa.student_user_id, principal.id
FROM student_fee_accounts fa
JOIN student_profiles sp ON sp.id = fa.student_id
CROSS JOIN LATERAL (SELECT substring(sp.admission_number FROM '[0-9]+$')::int AS n) x
JOIN users principal ON principal.email = 'principal@demo.erp'
WHERE fa.paid_amount > 0;

INSERT INTO fee_payments (
    created_at, updated_at, amount, payment_date, payment_mode, proof_url,
    remarks, status, submitted_at, transaction_reference, college_id,
    department_id, student_id, fee_account_id, student_user_id
)
SELECT now(), now(), 5000, DATE '2026-07-15', 'UPI',
       'demo://pending-proof/' || n, 'Pending verification demo', 'PENDING', now(),
       'DEMO-PENDING-' || lpad(n::text, 3, '0'), fa.college_id, fa.department_id,
       fa.student_id, fa.id, fa.student_user_id
FROM student_fee_accounts fa
JOIN student_profiles sp ON sp.id = fa.student_id
CROSS JOIN LATERAL (SELECT substring(sp.admission_number FROM '[0-9]+$')::int AS n) x
WHERE n % 10 = 0;

INSERT INTO fee_transactions (
    created_at, updated_at, amount, new_paid_amount, new_remaining_amount,
    previous_paid_amount, previous_remaining_amount, remarks, transaction_type,
    fee_payment_id, performed_by_id, fee_account_id
)
SELECT fp.verified_at, fp.verified_at, fp.amount, fa.paid_amount, fa.remaining_amount,
       0, fa.total_fee, 'Demo verified payment transaction', 'PAYMENT_VERIFIED',
       fp.id, fp.verified_by_id, fa.id
FROM fee_payments fp
JOIN student_fee_accounts fa ON fa.id = fp.fee_account_id
WHERE fp.status = 'VERIFIED';

INSERT INTO notices (created_at, updated_at, title, message, college_id, created_by_user_id)
SELECT now() - INTERVAL '2 days', now(), v.title, v.message, c.id, p.id
FROM colleges c
JOIN users p ON p.email = 'principal@demo.erp'
CROSS JOIN (VALUES
    ('Welcome to Academic Year 2026-2027', 'Welcome students and staff. Please review your dashboard and timetable.'),
    ('Fee Verification Window', 'Students with pending payments should contact the fee section.'),
    ('Attendance Review', 'Class teachers should verify weekly attendance before submission.')
) AS v(title, message);

INSERT INTO notice_audience_roles (notice_id, role_name)
SELECT n.id, role_name
FROM notices n
CROSS JOIN unnest(ARRAY['HOD','CLASS_TEACHER','SUBJECT_TEACHER','STUDENT']) AS role_name;

WITH chosen_entries AS (
    SELECT DISTINCT ON (div.id)
           div.id AS section_id, wt.college_id, e.id AS entry_id, e.subject_id,
           e.teacher_id, p.start_time, p.end_time,
           row_number() OVER (ORDER BY div.id) AS rn
    FROM course_year_divisions div
    JOIN weekly_timetables wt ON wt.section_id = div.id
    JOIN weekly_timetable_entries e ON e.timetable_id = wt.id AND e.day_of_week = 'MONDAY'
    JOIN weekly_timetable_periods p ON p.id = e.period_id
    ORDER BY div.id, p.position
)
INSERT INTO weekly_attendance_sessions (
    created_at, updated_at, attendance_date, end_time, lecture_number,
    start_time, status, submitted_at, college_id, section_id, subject_id,
    teacher_id, timetable_entry_id
)
SELECT now(), now(), DATE '2026-07-13', end_time, rn, start_time, 'SUBMITTED',
       now(), college_id, section_id, subject_id, teacher_id, entry_id
FROM chosen_entries;

INSERT INTO weekly_attendance_records (
    created_at, updated_at, remarks, status, session_id, student_id
)
SELECT now(), now(), 'Seeded review attendance',
       (ARRAY['PRESENT','PRESENT','PRESENT','ABSENT','LATE'])[((sp.id - 1) % 5) + 1],
       session.id, sp.id
FROM weekly_attendance_sessions session
JOIN student_section_enrollments enrollment ON enrollment.section_id = session.section_id
JOIN student_profiles sp ON sp.id = enrollment.student_id;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users), true);

COMMIT;

\echo 'Full review dataset created successfully.'
\echo 'Demo user passwords are supplied separately; no password is stored in this script.'
