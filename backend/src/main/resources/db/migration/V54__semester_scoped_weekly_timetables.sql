-- Keep historical timetables while allowing one live and one draft timetable
-- for the same division in each semester offering.
DROP INDEX IF EXISTS uk_weekly_timetable_active_section;
DROP INDEX IF EXISTS uk_weekly_timetable_draft_section;

-- Records from a non-current offering are history and must not participate in
-- current dashboards, teacher schedules, clash checks, or attendance.
UPDATE weekly_timetables timetable
SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP
FROM semester_offerings offering
WHERE offering.id = timetable.semester_offering_id
  AND offering.status <> 'ACTIVE'
  AND timetable.status <> 'ARCHIVED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_timetable_active_section_semester
    ON weekly_timetables (section_id, semester_offering_id)
    WHERE status = 'ACTIVE' AND semester_offering_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_timetable_draft_section_semester
    ON weekly_timetables (section_id, semester_offering_id)
    WHERE status = 'DRAFT' AND semester_offering_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_weekly_timetable_current_semester
    ON weekly_timetables (section_id, semester_offering_id, status, review_status);
