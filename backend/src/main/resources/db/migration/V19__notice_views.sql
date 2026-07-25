CREATE TABLE IF NOT EXISTS notice_views (
    id BIGSERIAL PRIMARY KEY,
    notice_id BIGINT NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seen_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_notice_view_user UNIQUE (notice_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_notice_views_user ON notice_views(user_id);
