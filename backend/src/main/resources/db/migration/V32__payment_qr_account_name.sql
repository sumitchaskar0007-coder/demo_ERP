ALTER TABLE colleges
    ADD COLUMN IF NOT EXISTS payment_qr_account_name VARCHAR(150);

UPDATE colleges
SET payment_qr_account_name = name
WHERE qr_code_url IS NOT NULL
  AND (payment_qr_account_name IS NULL OR BTRIM(payment_qr_account_name) = '');

