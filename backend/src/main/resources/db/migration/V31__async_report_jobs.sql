CREATE TABLE report_export_jobs (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    requester_user_id bigint NOT NULL,
    requester_college_id bigint,
    scope_college_id bigint NOT NULL,
    scope_department_id bigint,
    requester_role varchar(40) NOT NULL,
    report_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    status_filter varchar(40),
    idempotency_key_hash varchar(64) NOT NULL,
    request_fingerprint varchar(64) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL,
    next_attempt_at timestamp,
    processing_started_at timestamp,
    completed_at timestamp,
    expires_at timestamp,
    result_key varchar(400),
    result_filename varchar(180),
    result_content_type varchar(100),
    result_size_bytes bigint,
    failure_reason varchar(250),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT fk_report_job_requester
        FOREIGN KEY (requester_user_id) REFERENCES users(id),
    CONSTRAINT fk_report_job_requester_college
        FOREIGN KEY (requester_college_id) REFERENCES colleges(id),
    CONSTRAINT fk_report_job_scope_college
        FOREIGN KEY (scope_college_id) REFERENCES colleges(id),
    CONSTRAINT fk_report_job_scope_department
        FOREIGN KEY (scope_department_id) REFERENCES departments(id),
    CONSTRAINT uk_report_job_owner_idempotency
        UNIQUE (requester_user_id, idempotency_key_hash),
    CONSTRAINT ck_report_job_type
        CHECK (report_type IN ('ADMISSIONS', 'FEES', 'ATTENDANCE', 'STUDENTS')),
    CONSTRAINT ck_report_job_status
        CHECK (status IN (
            'QUEUED', 'PROCESSING', 'RETRY_PENDING',
            'COMPLETED', 'FAILED', 'EXPIRED'
        )),
    CONSTRAINT ck_report_job_attempts
        CHECK (
            attempt_count >= 0
            AND max_attempts BETWEEN 1 AND 10
            AND attempt_count <= max_attempts
        ),
    CONSTRAINT ck_report_job_result_size
        CHECK (result_size_bytes IS NULL OR result_size_bytes >= 0),
    CONSTRAINT ck_report_job_hashes
        CHECK (
            idempotency_key_hash ~ '^[0-9a-f]{64}$'
            AND request_fingerprint ~ '^[0-9a-f]{64}$'
        ),
    CONSTRAINT ck_report_job_result
        CHECK (
            (status = 'COMPLETED'
                AND result_key IS NOT NULL
                AND result_filename IS NOT NULL
                AND result_content_type IS NOT NULL
                AND result_size_bytes IS NOT NULL
                AND completed_at IS NOT NULL
                AND expires_at IS NOT NULL)
            OR status <> 'COMPLETED'
        )
);

CREATE INDEX idx_report_job_status_retry
    ON report_export_jobs (status, next_attempt_at);

CREATE INDEX idx_report_job_owner_created
    ON report_export_jobs (requester_user_id, created_at DESC);

CREATE INDEX idx_report_job_expiry
    ON report_export_jobs (status, expires_at)
    WHERE status = 'COMPLETED';
