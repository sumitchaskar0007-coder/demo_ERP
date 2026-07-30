ALTER TABLE student_fee_accounts
    ALTER COLUMN fee_structure_id DROP NOT NULL;

INSERT INTO student_fee_accounts (
    created_at, updated_at, version, admission_form_id, student_id, student_user_id,
    college_id, department_id, fee_structure_id, academic_year, student_category,
    total_fee, paid_amount, remaining_amount, discount_amount,
    minimum_amount_for_admission, status
)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, admission.id, admission.student_id,
       admission.student_user_id, admission.college_id, admission.department_id, NULL,
       admission.academic_year, COALESCE(admission.student_category, 'OPEN'),
       department.admission_form_fee, 0, department.admission_form_fee, 0,
       department.admission_form_fee, 'PENDING'
FROM admission_forms admission
JOIN departments department ON department.id = admission.department_id
WHERE admission.details_completed_at IS NOT NULL
  AND department.admission_form_fee > 0
  AND NOT EXISTS (
      SELECT 1 FROM student_fee_accounts account
      WHERE account.admission_form_id = admission.id
  );
