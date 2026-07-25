ALTER TABLE admission_forms
    ADD COLUMN IF NOT EXISTS tenth_marksheet_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS twelfth_marksheet_storage_name varchar(180),
    ADD COLUMN IF NOT EXISTS photo_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS tenth_marksheet_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS twelfth_marksheet_verified boolean NOT NULL DEFAULT false;

DO $$
DECLARE constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'admission_forms'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%STUDENT_SECTION_REVIEW_PENDING%'
    LOOP
        EXECUTE format('ALTER TABLE admission_forms DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE admission_forms
    ADD CONSTRAINT admission_forms_status_check CHECK (status IN (
        'STUDENT_DETAILS_PENDING', 'SUBMITTED', 'STUDENT_SECTION_REVIEW_PENDING',
        'STUDENT_SECTION_APPROVED', 'STUDENT_SECTION_REJECTED', 'PRINCIPAL_REVIEW_PENDING',
        'PRINCIPAL_APPROVED', 'PRINCIPAL_REJECTED', 'CANCELLED'
    ));

ALTER TABLE admission_status_history DROP CONSTRAINT IF EXISTS admission_status_history_action_check;
ALTER TABLE admission_status_history ADD CONSTRAINT admission_status_history_action_check CHECK (action IN (
    'STUDENT_DETAILS_SUBMITTED', 'SUBMITTED', 'STUDENT_SECTION_REVIEW_STARTED',
    'STUDENT_SECTION_APPROVED', 'STUDENT_SECTION_REJECTED', 'ADMISSION_FORM_PRINTED',
    'STATUS_UPDATED', 'FEE_ACCOUNT_CREATED', 'PAYMENT_SUBMITTED', 'PAYMENT_VERIFIED',
    'PAYMENT_REJECTED', 'PRINCIPAL_REVIEW_PENDING', 'PRINCIPAL_APPROVED', 'PRINCIPAL_REJECTED'
));
