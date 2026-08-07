ALTER TABLE fee_transactions
    DROP CONSTRAINT IF EXISTS fee_transactions_transaction_type_check;

ALTER TABLE fee_transactions
    ADD CONSTRAINT fee_transactions_transaction_type_check
    CHECK (transaction_type IN (
        'PAYMENT_VERIFIED',
        'PAYMENT_REJECTED',
        'DISCOUNT_APPLIED',
        'SCHOLARSHIP_APPROVED',
        'SCHOLARSHIP_REMOVED',
        'FEE_CATEGORY_CHANGED',
        'FEE_ADJUSTMENT',
        'REFUND'
    ));
