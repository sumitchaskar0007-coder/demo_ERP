UPDATE student_fee_accounts AS account
SET total_fee = department.admission_form_fee,
    remaining_amount = department.admission_form_fee,
    minimum_amount_for_admission = department.admission_form_fee,
    updated_at = CURRENT_TIMESTAMP,
    version = account.version + 1
FROM departments AS department
WHERE account.department_id = department.id
  AND account.fee_structure_id IS NULL
  AND account.status = 'PENDING'
  AND account.paid_amount = 0
  AND department.admission_form_fee > 0
  AND (
      account.total_fee <> department.admission_form_fee
      OR account.remaining_amount <> department.admission_form_fee
      OR account.minimum_amount_for_admission <> department.admission_form_fee
  )
  AND NOT EXISTS (
      SELECT 1
      FROM fee_payments AS payment
      WHERE payment.fee_account_id = account.id
  );
