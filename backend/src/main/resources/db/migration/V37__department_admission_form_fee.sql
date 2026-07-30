ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS admission_form_fee NUMERIC(12,2);

UPDATE departments
SET admission_form_fee = 0
WHERE admission_form_fee IS NULL;

ALTER TABLE departments
    ALTER COLUMN admission_form_fee SET DEFAULT 0,
    ALTER COLUMN admission_form_fee SET NOT NULL;

ALTER TABLE departments
    DROP CONSTRAINT IF EXISTS departments_admission_form_fee_check;

ALTER TABLE departments
    ADD CONSTRAINT departments_admission_form_fee_check CHECK (admission_form_fee >= 0);
