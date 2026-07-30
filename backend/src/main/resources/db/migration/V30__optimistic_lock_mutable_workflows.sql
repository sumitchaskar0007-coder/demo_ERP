-- Protect mutable admission workflow state and fee-structure administration
-- from overlapping lost updates. Existing rows start at version zero.
ALTER TABLE admission_forms
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE fee_structures
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
