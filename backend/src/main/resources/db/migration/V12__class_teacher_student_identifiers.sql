ALTER TABLE student_profiles
    ADD COLUMN IF NOT EXISTS prn_number VARCHAR(60);

ALTER TABLE student_profiles
    DROP CONSTRAINT IF EXISTS uk23wdqfc85p2k2fjkofadnl4m1;

ALTER TABLE student_section_enrollments
    ALTER COLUMN roll_number DROP NOT NULL;

UPDATE student_section_enrollments
SET roll_number = NULL
WHERE roll_number LIKE 'PENDING-%';

UPDATE student_profiles
SET roll_number = NULL
WHERE roll_number LIKE 'PENDING-%';

CREATE UNIQUE INDEX IF NOT EXISTS uk_student_profiles_prn_number
    ON student_profiles (LOWER(prn_number))
    WHERE prn_number IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_section_enrollment_roll_number
    ON student_section_enrollments (section_id, LOWER(roll_number))
    WHERE status = 'ACTIVE' AND roll_number IS NOT NULL;
