ALTER TABLE student_fee_accounts
    ADD COLUMN IF NOT EXISTS scholarship_removed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scholarship_removed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS scholarship_removal_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS credit_amount NUMERIC(12,2) NOT NULL DEFAULT 0;

UPDATE student_fee_accounts
SET credit_amount = GREATEST(paid_amount - (total_fee - discount_amount), 0)
WHERE credit_amount = 0
  AND paid_amount > (total_fee - discount_amount);

ALTER TABLE student_fee_accounts
    DROP CONSTRAINT IF EXISTS student_fee_accounts_credit_amount_check;

ALTER TABLE student_fee_accounts
    ADD CONSTRAINT student_fee_accounts_credit_amount_check CHECK (credit_amount >= 0);
