UPDATE fee_structures
SET status = 'INACTIVE'
WHERE gender IS NULL
  AND status = 'ACTIVE';

ALTER TABLE fee_structures
    DROP CONSTRAINT IF EXISTS fee_structures_active_gender_check;

ALTER TABLE fee_structures
    ADD CONSTRAINT fee_structures_active_gender_check
        CHECK (gender IS NOT NULL OR status = 'INACTIVE') NOT VALID;

ALTER TABLE fee_structures
    VALIDATE CONSTRAINT fee_structures_active_gender_check;
