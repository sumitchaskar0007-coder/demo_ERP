ALTER TABLE admission_documents
    ADD COLUMN sha256_checksum varchar(64),
    ADD COLUMN verified_at timestamp with time zone;

ALTER TABLE admission_documents
    ADD CONSTRAINT ck_admission_document_sha256
        CHECK (sha256_checksum IS NULL OR sha256_checksum ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_admission_document_verification_pair
        CHECK (
            (sha256_checksum IS NULL AND verified_at IS NULL)
            OR (sha256_checksum IS NOT NULL AND verified_at IS NOT NULL)
        );

CREATE TABLE admission_document_uploads (
    id uuid PRIMARY KEY,
    admission_form_id bigint NOT NULL,
    document_type varchar(60) NOT NULL,
    storage_name varchar(220) NOT NULL,
    original_filename varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    expected_file_size bigint NOT NULL,
    expected_sha256 varchar(64) NOT NULL,
    expected_sha256_base64 varchar(44) NOT NULL,
    status varchar(20) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    failure_code varchar(64),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_admission_document_upload_form
        FOREIGN KEY (admission_form_id) REFERENCES admission_forms(id),
    CONSTRAINT uk_admission_document_upload_storage UNIQUE (storage_name),
    CONSTRAINT ck_admission_document_upload_size
        CHECK (expected_file_size > 0 AND expected_file_size <= 5242880),
    CONSTRAINT ck_admission_document_upload_sha256
        CHECK (expected_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_admission_document_upload_sha256_base64
        CHECK (expected_sha256_base64 ~ '^[A-Za-z0-9+/]{43}=$'),
    CONSTRAINT ck_admission_document_upload_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'QUARANTINED', 'EXPIRED')),
    CONSTRAINT ck_admission_document_upload_state_time
        CHECK (
            (status = 'COMPLETED' AND completed_at IS NOT NULL)
            OR (status <> 'COMPLETED' AND completed_at IS NULL)
        ),
    CONSTRAINT ck_admission_document_upload_safe_key
        CHECK (
            storage_name LIKE 'colleges/%/admission-documents/%'
            AND storage_name NOT LIKE '%..%'
            AND left(storage_name, 1) <> '/'
        )
);

CREATE INDEX idx_admission_document_upload_form_type
    ON admission_document_uploads(admission_form_id, document_type, status);

CREATE INDEX idx_admission_document_upload_expiry
    ON admission_document_uploads(status, expires_at);
