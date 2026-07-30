INSERT INTO student_fee_accounts (
    created_at, updated_at, version, admission_form_id, student_id, student_user_id,
    college_id, department_id, fee_structure_id, academic_year, student_category,
    total_fee, paid_amount, remaining_amount, discount_amount,
    minimum_amount_for_admission, status
)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, admission.id, admission.student_id,
       admission.student_user_id, admission.college_id, admission.department_id,
       structure.id, admission.academic_year, admission.student_category,
       structure.total_fee, 0, structure.total_fee, 0,
       structure.minimum_amount_for_admission, 'PENDING'
FROM admission_forms admission
JOIN LATERAL (
    SELECT fee.*
    FROM fee_structures fee
    WHERE fee.college_id = admission.college_id
      AND fee.department_id = admission.department_id
      AND fee.student_category = admission.student_category
      AND fee.status = 'ACTIVE'
      AND LEFT(fee.academic_year, 4) = LEFT(admission.academic_year, 4)
    ORDER BY
        CASE
            WHEN admission.course_year_id IS NOT NULL
             AND LOWER(TRIM(fee.course_year)) = LOWER(TRIM((
                 SELECT academic_class.name
                 FROM academic_classes academic_class
                 WHERE academic_class.id = admission.course_year_id
             ))) THEN 0
            ELSE 1
        END,
        fee.created_at DESC
    LIMIT 1
) structure ON TRUE
WHERE admission.status IN (
    'STUDENT_SECTION_APPROVED',
    'PRINCIPAL_REVIEW_PENDING',
    'PRINCIPAL_APPROVED'
)
  AND EXISTS (
      SELECT 1
      FROM student_fee_accounts admission_fee
      WHERE admission_fee.admission_form_id = admission.id
        AND admission_fee.fee_structure_id IS NULL
        AND admission_fee.paid_amount >= admission_fee.minimum_amount_for_admission
  )
  AND NOT EXISTS (
      SELECT 1
      FROM student_fee_accounts regular_fee
      WHERE regular_fee.admission_form_id = admission.id
        AND regular_fee.fee_structure_id IS NOT NULL
  );
