ALTER TABLE weekly_timetable_entries
    DROP CONSTRAINT IF EXISTS weekly_timetable_entries_lecture_type_check;

UPDATE weekly_timetable_entries
SET lecture_type = CASE lecture_type
    WHEN 'PRACTICAL' THEN 'LAB'
    WHEN 'TUTORIAL' THEN 'OTHER'
    ELSE lecture_type
END
WHERE lecture_type IN ('PRACTICAL', 'TUTORIAL');

ALTER TABLE weekly_timetable_entries
    ADD CONSTRAINT weekly_timetable_entries_lecture_type_check
    CHECK (lecture_type IN ('THEORY', 'LAB', 'OTHER'));
