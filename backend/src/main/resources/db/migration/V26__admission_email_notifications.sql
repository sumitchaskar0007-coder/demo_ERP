ALTER TABLE email_notifications
    DROP CONSTRAINT IF EXISTS email_notifications_email_type_check;

ALTER TABLE email_notifications
    ADD CONSTRAINT email_notifications_email_type_check
        CHECK (email_type IN (
            'USER_ACCOUNT_CREATED',
            'PRINCIPAL_ACCOUNT_CREATED',
            'ADMISSION_COMPLETED',
            'ADMISSION_APPROVED',
            'ACCOUNT_ACTIVATED',
            'ACCOUNT_DEACTIVATED',
            'PASSWORD_RESET',
            'PASSWORD_CHANGED',
            'VERIFY_EMAIL',
            'EMAIL_VERIFIED',
            'FEE_PAYMENT_REMINDER'
        ));
