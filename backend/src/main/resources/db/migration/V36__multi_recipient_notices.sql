ALTER TABLE notices ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'COMMON';
ALTER TABLE notices ADD CONSTRAINT notices_delivery_mode_check
    CHECK (delivery_mode IN ('COMMON', 'INDIVIDUAL'));
UPDATE notices SET delivery_mode = 'INDIVIDUAL' WHERE recipient_user_id IS NOT NULL;

CREATE TABLE notice_recipients (
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_notice_recipients PRIMARY KEY (notice_id, user_id),
    CONSTRAINT fk_notice_recipients_notice FOREIGN KEY (notice_id) REFERENCES notices(id) ON DELETE CASCADE,
    CONSTRAINT fk_notice_recipients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notice_recipients_user_notice ON notice_recipients(user_id, notice_id);

ALTER TABLE notice_audience_roles DROP CONSTRAINT IF EXISTS notice_audience_roles_role_name_check;
ALTER TABLE notice_audience_roles ADD CONSTRAINT notice_audience_roles_role_name_check
    CHECK (role_name IN (
        'SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'HOD', 'STUDENT_SECTION',
        'FEE_SECTION', 'CLASS_TEACHER', 'SUBJECT_TEACHER', 'GENERAL_STAFF', 'STUDENT'
    ));
