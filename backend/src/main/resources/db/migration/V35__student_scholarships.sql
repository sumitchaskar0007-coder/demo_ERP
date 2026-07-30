ALTER TABLE notices
    ADD COLUMN recipient_user_id BIGINT,
    ADD CONSTRAINT fk_notice_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users(id);

CREATE INDEX idx_notices_recipient_created
    ON notices(recipient_user_id, created_at DESC)
    WHERE deleted_at IS NULL;

ALTER TABLE fee_transactions
    DROP CONSTRAINT fee_transactions_transaction_type_check;

ALTER TABLE fee_transactions
    ADD CONSTRAINT fee_transactions_transaction_type_check
    CHECK (transaction_type IN (
        'PAYMENT_VERIFIED',
        'PAYMENT_REJECTED',
        'DISCOUNT_APPLIED',
        'SCHOLARSHIP_APPROVED',
        'FEE_ADJUSTMENT',
        'REFUND'
    ));
