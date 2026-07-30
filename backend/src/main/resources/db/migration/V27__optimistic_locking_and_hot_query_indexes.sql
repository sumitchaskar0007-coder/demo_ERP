ALTER TABLE users ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE admission_forms ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE student_fee_accounts ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE fee_payments ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE weekly_timetables ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_notice_views_user_notice
    ON notice_views (user_id, notice_id);
CREATE INDEX IF NOT EXISTS idx_notices_visible_created
    ON notices (deleted_at, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON refresh_tokens (user_id, revoked, expires_at);
CREATE INDEX IF NOT EXISTS idx_users_college_status
    ON users (college_id, status, id);
CREATE INDEX IF NOT EXISTS idx_email_notifications_claim
    ON email_notifications (status, next_retry_at, priority, created_at);
