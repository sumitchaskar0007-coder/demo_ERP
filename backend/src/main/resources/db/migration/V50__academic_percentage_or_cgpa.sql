ALTER TABLE admission_academic_records
    ADD COLUMN IF NOT EXISTS grading_type varchar(20),
    ADD COLUMN IF NOT EXISTS cgpa numeric(4,2);

UPDATE admission_academic_records
SET grading_type = 'PERCENTAGE'
WHERE grading_type IS NULL;

ALTER TABLE admission_academic_records
    ALTER COLUMN grading_type SET NOT NULL;

ALTER TABLE admission_academic_records
    DROP CONSTRAINT IF EXISTS admission_academic_records_result_check;

ALTER TABLE admission_academic_records
    ADD CONSTRAINT admission_academic_records_result_check CHECK (
        (
            grading_type = 'PERCENTAGE'
            AND cgpa IS NULL
            AND (marks_percentage IS NULL OR marks_percentage BETWEEN 0 AND 100)
        )
        OR (
            grading_type = 'CGPA'
            AND total_marks IS NULL
            AND obtained_marks IS NULL
            AND marks_percentage IS NULL
            AND (cgpa IS NULL OR cgpa BETWEEN 0 AND 10)
        )
    );
