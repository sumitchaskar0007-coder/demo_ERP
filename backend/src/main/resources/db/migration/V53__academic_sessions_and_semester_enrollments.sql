-- Additive semester/session model. Existing string academic-year columns remain
-- during the compatibility period so the release can be rolled back safely.

ALTER TABLE academic_years
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE academic_years
SET status = CASE
    WHEN active THEN 'ACTIVE'
    WHEN end_date < CURRENT_DATE THEN 'CLOSED'
    ELSE 'DRAFT'
END
WHERE status IS NULL;

ALTER TABLE academic_years
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE academic_years
    DROP CONSTRAINT IF EXISTS academic_years_status_check;
ALTER TABLE academic_years
    ADD CONSTRAINT academic_years_status_check
    CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'));

WITH ranked_active_years AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY college_id ORDER BY start_date DESC, id DESC) AS position
    FROM academic_years
    WHERE status = 'ACTIVE'
)
UPDATE academic_years year
SET status = 'DRAFT', active = FALSE
FROM ranked_active_years ranked
WHERE year.id = ranked.id AND ranked.position > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_year_active_college
    ON academic_years (college_id)
    WHERE status = 'ACTIVE';

ALTER TABLE academic_terms
    ADD COLUMN IF NOT EXISTS term_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- Preserve any pre-existing terms. Their parity is inferred from chronological
-- order only when it was not explicitly configured.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY academic_year_id ORDER BY start_date, id) AS position
    FROM academic_terms
)
UPDATE academic_terms term
SET term_type = CASE WHEN MOD(ranked.position, 2) = 1 THEN 'ODD' ELSE 'EVEN' END
FROM ranked
WHERE term.id = ranked.id AND term.term_type IS NULL;

UPDATE academic_terms term
SET status = CASE
    WHEN CURRENT_DATE BETWEEN term.start_date AND term.end_date THEN 'ACTIVE'
    WHEN term.end_date < CURRENT_DATE THEN 'CLOSED'
    ELSE 'PLANNED'
END
WHERE term.status IS NULL;

ALTER TABLE academic_terms
    ALTER COLUMN term_type SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'PLANNED',
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE academic_terms
    DROP CONSTRAINT IF EXISTS academic_terms_type_check;
ALTER TABLE academic_terms
    ADD CONSTRAINT academic_terms_type_check CHECK (term_type IN ('ODD', 'EVEN'));
ALTER TABLE academic_terms
    DROP CONSTRAINT IF EXISTS academic_terms_status_check;
ALTER TABLE academic_terms
    ADD CONSTRAINT academic_terms_status_check
    CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED'));
ALTER TABLE academic_terms
    DROP CONSTRAINT IF EXISTS academic_terms_dates_check;
ALTER TABLE academic_terms
    ADD CONSTRAINT academic_terms_dates_check CHECK (start_date <= end_date);

WITH ranked_active_terms AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY college_id ORDER BY start_date DESC, id DESC) AS position
    FROM academic_terms
    WHERE status = 'ACTIVE'
)
UPDATE academic_terms term
SET status = 'PLANNED'
FROM ranked_active_terms ranked
WHERE term.id = ranked.id AND ranked.position > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_term_type
    ON academic_terms (academic_year_id, term_type);
CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_term_active_college
    ON academic_terms (college_id)
    WHERE status = 'ACTIVE';

-- Ensure every academic year represented by the active course-year workflow has
-- a managed calendar record. Dates are safe draft defaults and remain editable.
WITH source_years AS (
    SELECT DISTINCT college_id, academic_year AS name
    FROM course_years
    WHERE academic_year ~ '[0-9]{4}'
    UNION
    SELECT DISTINCT section.college_id, enrollment.academic_year AS name
    FROM student_section_enrollments enrollment
    JOIN course_year_divisions section ON section.id = enrollment.section_id
    WHERE enrollment.academic_year ~ '[0-9]{4}'
), normalized AS (
    SELECT college_id, name,
           SUBSTRING(name FROM '([0-9]{4})')::INTEGER AS start_year
    FROM source_years
)
INSERT INTO academic_years
    (college_id, name, start_date, end_date, active, status, created_at, updated_at)
SELECT college_id, name, make_date(start_year, 7, 1), make_date(start_year + 1, 6, 30),
       FALSE, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM normalized
ON CONFLICT (college_id, name) DO NOTHING;

-- Seed missing odd/even calendar windows. Institutions can adjust the dates
-- before activation; no student is moved merely because these rows exist.
INSERT INTO academic_terms
    (college_id, academic_year_id, name, term_type, start_date, end_date,
     status, created_at, updated_at)
SELECT year.college_id, year.id, 'Odd Semester', 'ODD', year.start_date,
       LEAST(year.end_date, (year.start_date + INTERVAL '6 months - 1 day')::date),
       'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM academic_years year
ON CONFLICT (academic_year_id, term_type) DO NOTHING;

-- Preserve continuity for colleges that previously used only string academic
-- years: activate the one calendar containing today when no managed year/term
-- had already been activated.
WITH candidates AS (
    SELECT year.id, ROW_NUMBER() OVER (
        PARTITION BY year.college_id ORDER BY year.start_date DESC, year.id DESC) AS position
    FROM academic_years year
    WHERE CURRENT_DATE BETWEEN year.start_date AND year.end_date
      AND NOT EXISTS (
          SELECT 1 FROM academic_years active_year
          WHERE active_year.college_id = year.college_id
            AND active_year.status = 'ACTIVE')
)
UPDATE academic_years year
SET status = 'ACTIVE', active = TRUE
FROM candidates candidate
WHERE year.id = candidate.id AND candidate.position = 1;

INSERT INTO academic_terms
    (college_id, academic_year_id, name, term_type, start_date, end_date,
     status, created_at, updated_at)
SELECT year.college_id, year.id, 'Even Semester', 'EVEN',
       LEAST(year.end_date, (year.start_date + INTERVAL '6 months')::date),
       year.end_date, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM academic_years year
ON CONFLICT (academic_year_id, term_type) DO NOTHING;

WITH candidates AS (
    SELECT term.id, ROW_NUMBER() OVER (
        PARTITION BY term.college_id ORDER BY term.start_date DESC, term.id DESC) AS position
    FROM academic_terms term
    JOIN academic_years year ON year.id = term.academic_year_id
    WHERE year.status = 'ACTIVE'
      AND CURRENT_DATE BETWEEN term.start_date AND term.end_date
      AND NOT EXISTS (
          SELECT 1 FROM academic_terms active_term
          WHERE active_term.college_id = term.college_id
            AND active_term.status = 'ACTIVE')
)
UPDATE academic_terms term
SET status = 'ACTIVE'
FROM candidates candidate
WHERE term.id = candidate.id AND candidate.position = 1;

CREATE TABLE IF NOT EXISTS curriculum_semesters (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES colleges(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    semester_number INTEGER NOT NULL,
    year_name VARCHAR(30) NOT NULL,
    term_type VARCHAR(10) NOT NULL,
    name VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT curriculum_semester_number_check CHECK (semester_number BETWEEN 1 AND 10),
    CONSTRAINT curriculum_semester_year_check CHECK (year_name IN
        ('FIRST_YEAR','SECOND_YEAR','THIRD_YEAR','FOURTH_YEAR','FIFTH_YEAR')),
    CONSTRAINT curriculum_semester_term_check CHECK (term_type IN ('ODD','EVEN')),
    CONSTRAINT curriculum_semester_parity_check CHECK (
        (term_type = 'ODD' AND MOD(semester_number, 2) = 1) OR
        (term_type = 'EVEN' AND MOD(semester_number, 2) = 0)),
    CONSTRAINT uk_curriculum_semester_department_number
        UNIQUE (department_id, semester_number)
);

CREATE INDEX IF NOT EXISTS idx_curriculum_semester_scope
    ON curriculum_semesters (college_id, department_id, active, semester_number);

-- Infer each department's supported duration from configured course years.
WITH duration AS (
    SELECT department_id, MAX(CASE year_name
        WHEN 'FIRST_YEAR' THEN 1 WHEN 'SECOND_YEAR' THEN 2
        WHEN 'THIRD_YEAR' THEN 3 WHEN 'FOURTH_YEAR' THEN 4
        WHEN 'FIFTH_YEAR' THEN 5 ELSE 1 END) AS years
    FROM course_years
    GROUP BY department_id
), numbered AS (
    SELECT department.id AS department_id, department.college_id,
           generate_series(1, COALESCE(duration.years, 4) * 2) AS semester_number
    FROM departments department
    LEFT JOIN duration ON duration.department_id = department.id
)
INSERT INTO curriculum_semesters
    (college_id, department_id, semester_number, year_name, term_type,
     name, active, created_at, updated_at)
SELECT college_id, department_id, semester_number,
       CASE CEIL(semester_number / 2.0)::INTEGER
           WHEN 1 THEN 'FIRST_YEAR' WHEN 2 THEN 'SECOND_YEAR'
           WHEN 3 THEN 'THIRD_YEAR' WHEN 4 THEN 'FOURTH_YEAR'
           ELSE 'FIFTH_YEAR' END,
       CASE WHEN MOD(semester_number, 2) = 1 THEN 'ODD' ELSE 'EVEN' END,
       'Semester ' || semester_number, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM numbered
ON CONFLICT (department_id, semester_number) DO NOTHING;

CREATE TABLE IF NOT EXISTS semester_offerings (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES colleges(id),
    department_id BIGINT NOT NULL REFERENCES departments(id),
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    academic_term_id BIGINT NOT NULL REFERENCES academic_terms(id),
    curriculum_semester_id BIGINT NOT NULL REFERENCES curriculum_semesters(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT semester_offering_status_check
        CHECK (status IN ('PLANNED','ACTIVE','CLOSED')),
    CONSTRAINT uk_semester_offering UNIQUE (academic_term_id, curriculum_semester_id)
);

CREATE INDEX IF NOT EXISTS idx_semester_offering_scope
    ON semester_offerings (college_id, department_id, academic_year_id, status);

INSERT INTO semester_offerings
    (college_id, department_id, academic_year_id, academic_term_id,
     curriculum_semester_id, status, created_at, updated_at)
SELECT DISTINCT year.college_id, course.department_id, year.id, term.id,
       semester.id,
       CASE term.status WHEN 'ACTIVE' THEN 'ACTIVE'
                        WHEN 'CLOSED' THEN 'CLOSED' ELSE 'PLANNED' END,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM course_years course
JOIN academic_years year
  ON year.college_id = course.college_id
 AND replace(year.name, '/', '-') = replace(course.academic_year, '/', '-')
JOIN curriculum_semesters semester
  ON semester.department_id = course.department_id AND semester.year_name = course.year_name
JOIN academic_terms term
  ON term.academic_year_id = year.id AND term.term_type = semester.term_type
ON CONFLICT (academic_term_id, curriculum_semester_id) DO NOTHING;

UPDATE semester_offerings offering
SET status = CASE term.status WHEN 'ACTIVE' THEN 'ACTIVE'
                              WHEN 'CLOSED' THEN 'CLOSED' ELSE 'PLANNED' END
FROM academic_terms term
WHERE term.id = offering.academic_term_id;

ALTER TABLE student_section_enrollments
    ADD COLUMN IF NOT EXISTS semester_offering_id BIGINT REFERENCES semester_offerings(id),
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completion_status VARCHAR(20);

ALTER TABLE student_section_enrollments
    DROP CONSTRAINT IF EXISTS student_enrollment_completion_status_check;
ALTER TABLE student_section_enrollments
    ADD CONSTRAINT student_enrollment_completion_status_check
    CHECK (completion_status IS NULL OR completion_status IN
        ('COMPLETED','PROMOTED','REPEAT','HELD','GRADUATED','WITHDRAWN'));

-- The former student/year uniqueness cannot represent both odd and even
-- semester history in the same academic year.
DO $$
DECLARE constraint_name TEXT;
BEGIN
    SELECT pc.conname INTO constraint_name
    FROM pg_constraint pc
    WHERE pc.conrelid = 'student_section_enrollments'::regclass
      AND pc.contype = 'u'
      AND (
          SELECT ARRAY_AGG(attribute.attname::TEXT ORDER BY attribute.attname::TEXT)
          FROM UNNEST(pc.conkey) key(attnum)
          JOIN pg_attribute attribute
            ON attribute.attrelid = pc.conrelid
           AND attribute.attnum = key.attnum
      ) = ARRAY['academic_year', 'student_id'];
    IF constraint_name IS NOT NULL THEN
        EXECUTE FORMAT('ALTER TABLE student_section_enrollments DROP CONSTRAINT %I',
                       constraint_name);
    END IF;
END $$;

-- Backfill the currently active semester from configured calendar dates. If no
-- term is active by date, odd is selected as the non-destructive default.
WITH selected_term AS (
    SELECT year.id AS academic_year_id,
           COALESCE(
               MAX(term.id) FILTER (WHERE CURRENT_DATE BETWEEN term.start_date AND term.end_date),
               MAX(term.id) FILTER (WHERE term.term_type = 'ODD')) AS term_id
    FROM academic_years year
    JOIN academic_terms term ON term.academic_year_id = year.id
    GROUP BY year.id
), enrollment_offering AS (
    SELECT enrollment.id AS enrollment_id, offering.id AS offering_id
    FROM student_section_enrollments enrollment
    JOIN course_year_divisions division ON division.id = enrollment.section_id
    JOIN course_years course ON course.id = division.academic_class_id
    JOIN academic_years year
      ON year.college_id = division.college_id AND year.name = enrollment.academic_year
    JOIN selected_term selected ON selected.academic_year_id = year.id
    JOIN semester_offerings offering
      ON offering.academic_year_id = year.id
     AND offering.department_id = division.department_id
     AND offering.academic_term_id = selected.term_id
    JOIN curriculum_semesters semester
      ON semester.id = offering.curriculum_semester_id
     AND semester.year_name = course.year_name
    WHERE enrollment.semester_offering_id IS NULL
)
UPDATE student_section_enrollments enrollment
SET semester_offering_id = mapping.offering_id
FROM enrollment_offering mapping
WHERE enrollment.id = mapping.enrollment_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_semester_enrollment
    ON student_section_enrollments (student_id, semester_offering_id)
    WHERE semester_offering_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_student_active_semester_enrollment
    ON student_section_enrollments (student_id, status);
CREATE INDEX IF NOT EXISTS idx_student_enrollment_semester
    ON student_section_enrollments (semester_offering_id, status, section_id);

ALTER TABLE course_year_subjects
    ADD COLUMN IF NOT EXISTS curriculum_semester_id BIGINT REFERENCES curriculum_semesters(id);
UPDATE course_year_subjects subject
SET curriculum_semester_id = (
    SELECT semester.id
    FROM course_years course_year
    JOIN academic_years academic_year
      ON academic_year.college_id = course_year.college_id
     AND replace(academic_year.name, '/', '-') = replace(course_year.academic_year, '/', '-')
    JOIN academic_terms term ON term.academic_year_id = academic_year.id
    JOIN curriculum_semesters semester
      ON semester.department_id = course_year.department_id
     AND semester.year_name = course_year.year_name
     AND semester.term_type = term.term_type
    WHERE course_year.id = subject.academic_class_id
    ORDER BY CASE
        WHEN term.status = 'ACTIVE' THEN 0
        WHEN CURRENT_DATE BETWEEN term.start_date AND term.end_date THEN 1
        WHEN term.term_type = 'ODD' THEN 2
        ELSE 3 END
    LIMIT 1
)
WHERE subject.curriculum_semester_id IS NULL;
ALTER TABLE course_year_subjects DROP CONSTRAINT IF EXISTS uktaeend7flmie6b8w1vpd8c89b;
CREATE UNIQUE INDEX IF NOT EXISTS uk_course_year_subject_semester_code
    ON course_year_subjects (academic_class_id, academic_year, curriculum_semester_id, lower(code))
    WHERE curriculum_semester_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_course_year_subject_semester
    ON course_year_subjects (curriculum_semester_id, status);

ALTER TABLE weekly_timetables
    ADD COLUMN IF NOT EXISTS semester_offering_id BIGINT REFERENCES semester_offerings(id);
WITH selected_term AS (
    SELECT year.id AS academic_year_id,
           COALESCE(
               MAX(term.id) FILTER (WHERE term.status = 'ACTIVE'),
               MAX(term.id) FILTER (WHERE CURRENT_DATE BETWEEN term.start_date AND term.end_date),
               MAX(term.id) FILTER (WHERE term.term_type = 'ODD')) AS term_id
    FROM academic_years year
    JOIN academic_terms term ON term.academic_year_id = year.id
    GROUP BY year.id
), timetable_offering AS (
    SELECT timetable.id AS timetable_id, offering.id AS offering_id
    FROM weekly_timetables timetable
    JOIN course_year_divisions division ON division.id = timetable.section_id
    JOIN course_years course ON course.id = division.academic_class_id
    JOIN academic_years year
      ON year.college_id = division.college_id
     AND replace(year.name, '/', '-') = replace(division.academic_year, '/', '-')
    JOIN selected_term selected ON selected.academic_year_id = year.id
    JOIN semester_offerings offering
      ON offering.academic_year_id = year.id
     AND offering.department_id = division.department_id
     AND offering.academic_term_id = selected.term_id
    JOIN curriculum_semesters semester
      ON semester.id = offering.curriculum_semester_id
     AND semester.year_name = course.year_name
    WHERE timetable.semester_offering_id IS NULL
)
UPDATE weekly_timetables timetable
SET semester_offering_id = mapping.offering_id
FROM timetable_offering mapping
WHERE timetable.id = mapping.timetable_id;
CREATE INDEX IF NOT EXISTS idx_weekly_timetable_semester
    ON weekly_timetables (semester_offering_id, status);

ALTER TABLE student_fee_accounts
    ADD COLUMN IF NOT EXISTS semester_offering_id BIGINT REFERENCES semester_offerings(id);
ALTER TABLE fee_structures
    ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(20) NOT NULL DEFAULT 'ANNUAL';
ALTER TABLE fee_structures
    DROP CONSTRAINT IF EXISTS fee_structures_billing_cycle_check;
ALTER TABLE fee_structures
    ADD CONSTRAINT fee_structures_billing_cycle_check
    CHECK (billing_cycle IN ('ANNUAL','SEMESTER'));
CREATE INDEX IF NOT EXISTS idx_fee_account_semester
    ON student_fee_accounts (semester_offering_id, status);

CREATE TABLE IF NOT EXISTS semester_rollover_jobs (
    id BIGSERIAL PRIMARY KEY,
    college_id BIGINT NOT NULL REFERENCES colleges(id),
    source_term_id BIGINT NOT NULL REFERENCES academic_terms(id),
    target_term_id BIGINT NOT NULL REFERENCES academic_terms(id),
    status VARCHAR(20) NOT NULL,
    total_students INTEGER NOT NULL DEFAULT 0,
    promoted_students INTEGER NOT NULL DEFAULT 0,
    held_students INTEGER NOT NULL DEFAULT 0,
    graduated_students INTEGER NOT NULL DEFAULT 0,
    requested_by BIGINT REFERENCES users(id),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT semester_rollover_status_check
        CHECK (status IN ('PREVIEWED','RUNNING','COMPLETED','FAILED')),
    CONSTRAINT semester_rollover_distinct_terms_check CHECK (source_term_id <> target_term_id),
    CONSTRAINT uk_semester_rollover UNIQUE (college_id, source_term_id, target_term_id)
);

CREATE TABLE IF NOT EXISTS semester_rollover_items (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES semester_rollover_jobs(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES student_profiles(id),
    source_enrollment_id BIGINT NOT NULL REFERENCES student_section_enrollments(id),
    target_enrollment_id BIGINT REFERENCES student_section_enrollments(id),
    decision VARCHAR(20) NOT NULL,
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT semester_rollover_item_decision_check
        CHECK (decision IN ('PROMOTE','HOLD','REPEAT','GRADUATE','FAILED')),
    CONSTRAINT uk_semester_rollover_item UNIQUE (job_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_semester_rollover_job_status
    ON semester_rollover_jobs (college_id, status, created_at DESC);
