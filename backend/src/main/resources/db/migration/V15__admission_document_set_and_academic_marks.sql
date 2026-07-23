ALTER TABLE admission_forms
    ADD COLUMN IF NOT EXISTS graduation_pg_certificate_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS leaving_certificate_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS migration_certificate_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS gap_affidavit_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS caste_certificate_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS income_proof_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS name_change_certificate_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS aadhaar_card_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS leaving_certificate_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS aadhaar_card_verified boolean NOT NULL DEFAULT false;

ALTER TABLE admission_academic_records
    ADD COLUMN IF NOT EXISTS total_marks numeric(10,2),
    ADD COLUMN IF NOT EXISTS obtained_marks numeric(10,2);

ALTER TABLE admission_academic_records DROP CONSTRAINT IF EXISTS admission_academic_records_marks_check;
ALTER TABLE admission_academic_records ADD CONSTRAINT admission_academic_records_marks_check CHECK (
    (total_marks IS NULL AND obtained_marks IS NULL)
    OR (total_marks > 0 AND obtained_marks >= 0 AND obtained_marks <= total_marks)
);
