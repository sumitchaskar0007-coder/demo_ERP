ALTER TABLE admission_forms
    ADD COLUMN detail_draft JSONB,
    ADD COLUMN detail_draft_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN detail_draft_updated_at TIMESTAMP;

ALTER TABLE admission_forms
    ADD CONSTRAINT admission_detail_draft_object_check
    CHECK (detail_draft IS NULL OR jsonb_typeof(detail_draft) = 'object');
