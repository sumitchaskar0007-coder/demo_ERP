-- Timetables and subject assignments reference staff_profiles. Give every
-- existing Principal a college-wide teaching profile without changing their
-- PRINCIPAL authorization role or requiring a department assignment.
INSERT INTO staff_profiles (
    created_at, updated_at, email, employee_code, full_name, joining_date,
    phone, staff_type, status, college_id, department_id, user_id
)
SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, principal.email,
       'PRINCIPAL-' || principal.id, principal.full_name, CURRENT_DATE,
       principal.phone, 'TEACHER', principal.status, principal.college_id, NULL,
       principal.id
FROM users principal
JOIN user_roles membership ON membership.user_id = principal.id
JOIN roles role ON role.id = membership.role_id AND role.name = 'PRINCIPAL'
WHERE principal.college_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM staff_profiles profile WHERE profile.user_id = principal.id
  )
ON CONFLICT (user_id) DO NOTHING;
