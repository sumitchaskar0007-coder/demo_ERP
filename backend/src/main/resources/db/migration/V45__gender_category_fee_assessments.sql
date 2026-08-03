ALTER TABLE fee_structures
    ADD COLUMN gender VARCHAR(10),
    ADD COLUMN custom_category_name VARCHAR(80),
    ADD COLUMN scholarship_amount NUMERIC(12,2) NOT NULL DEFAULT 0;

ALTER TABLE fee_structures
    ADD CONSTRAINT fee_structures_gender_check CHECK (gender IN ('MALE', 'FEMALE')),
    ADD CONSTRAINT fee_structures_scholarship_check CHECK (scholarship_amount >= 0 AND scholarship_amount <= total_fee),
    ADD CONSTRAINT fee_structures_custom_category_check CHECK (
        gender IS NULL
        OR (student_category = 'OTHER' AND custom_category_name IS NOT NULL AND btrim(custom_category_name) <> '')
        OR (student_category <> 'OTHER' AND custom_category_name IS NULL));

ALTER TABLE admission_forms ADD COLUMN custom_category_name VARCHAR(80);
ALTER TABLE student_profiles ADD COLUMN custom_category_name VARCHAR(80);
ALTER TABLE student_fee_accounts ADD COLUMN custom_category_name VARCHAR(80);

CREATE UNIQUE INDEX uq_active_fee_structure_assessment
    ON fee_structures (college_id, department_id, academic_year,
        lower(coalesce(course_year, 'First Year')), student_category,
        lower(coalesce(custom_category_name, '')), gender)
    WHERE status = 'ACTIVE' AND gender IS NOT NULL;

CREATE INDEX idx_fee_structure_public_categories
    ON fee_structures (college_id, department_id, status, student_category, custom_category_name);
CREATE INDEX idx_fee_accounts_structure_recalculation
    ON student_fee_accounts (fee_structure_id, id) WHERE fee_structure_id IS NOT NULL;
