ALTER TABLE weekly_timetables
    DROP CONSTRAINT IF EXISTS uk_weekly_timetable_section;

UPDATE weekly_timetables
SET status = 'DRAFT'
WHERE review_status <> 'APPROVED';

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_timetable_active_section
    ON weekly_timetables(section_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS uk_weekly_timetable_draft_section
    ON weekly_timetables(section_id)
    WHERE status = 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_weekly_timetable_section_status
    ON weekly_timetables(section_id, status);
