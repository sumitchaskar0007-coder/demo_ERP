DO $$
DECLARE constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'fee_payments'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE fee_payments DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE fee_payments
    ADD CONSTRAINT chk_fee_payment_status
    CHECK (status IN ('PENDING','VERIFIED','REJECTED','RESUBMISSION_REQUESTED','CANCELLED'));
