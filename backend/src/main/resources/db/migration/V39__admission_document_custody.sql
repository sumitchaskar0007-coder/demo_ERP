CREATE TABLE IF NOT EXISTS admission_document_custody (
    id BIGSERIAL PRIMARY KEY,
    admission_form_id BIGINT NOT NULL REFERENCES admission_forms(id) ON DELETE CASCADE,
    document_type VARCHAR(60) NOT NULL,
    original_received BOOLEAN NOT NULL DEFAULT FALSE,
    xerox_received BOOLEAN NOT NULL DEFAULT FALSE,
    received_by BIGINT REFERENCES users(id),
    received_at TIMESTAMP,
    returned_to_student BOOLEAN NOT NULL DEFAULT FALSE,
    returned_by BIGINT REFERENCES users(id),
    returned_at TIMESTAMP,
    return_remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admission_document_custody UNIQUE (admission_form_id, document_type),
    CONSTRAINT admission_document_custody_received_check
        CHECK (original_received OR xerox_received)
);
CREATE INDEX IF NOT EXISTS idx_admission_document_custody_admission
    ON admission_document_custody(admission_form_id);
