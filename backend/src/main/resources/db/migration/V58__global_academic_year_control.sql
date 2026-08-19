-- Global academic-year catalogue owned by the Super Admin. College academic
-- years remain tenant-owned and are linked additively for rollback safety.
CREATE TABLE IF NOT EXISTS global_academic_years (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    activated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_global_academic_year_name UNIQUE (name),
    CONSTRAINT ck_global_academic_year_dates CHECK (start_date <= end_date),
    CONSTRAINT ck_global_academic_year_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

ALTER TABLE academic_years
    ADD COLUMN IF NOT EXISTS global_academic_year_id BIGINT;

ALTER TABLE academic_years
    DROP CONSTRAINT IF EXISTS fk_academic_year_global;
ALTER TABLE academic_years
    ADD CONSTRAINT fk_academic_year_global
    FOREIGN KEY (global_academic_year_id) REFERENCES global_academic_years(id);

CREATE INDEX IF NOT EXISTS idx_academic_year_global
    ON academic_years (global_academic_year_id);

-- Preserve existing production calendars by creating one global catalogue row
-- per normalized year name. Existing college rows are never replaced.
INSERT INTO global_academic_years
    (name, start_date, end_date, status, created_at, updated_at)
SELECT replace(name, '/', '-'), MIN(start_date), MAX(end_date), 'DRAFT',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM academic_years
GROUP BY replace(name, '/', '-')
ON CONFLICT (name) DO NOTHING;

UPDATE academic_years college_year
SET global_academic_year_id = global_year.id
FROM global_academic_years global_year
WHERE college_year.global_academic_year_id IS NULL
  AND replace(college_year.name, '/', '-') = global_year.name;

-- Select at most one existing calendar as the global current year. Prefer the
-- year already active in the most colleges, then the latest calendar.
WITH candidate AS (
    SELECT global_year.id
    FROM global_academic_years global_year
    LEFT JOIN academic_years college_year
      ON college_year.global_academic_year_id = global_year.id
    GROUP BY global_year.id, global_year.start_date
    ORDER BY COUNT(*) FILTER (WHERE college_year.status = 'ACTIVE') DESC,
             (CURRENT_DATE BETWEEN MIN(global_year.start_date) AND MAX(global_year.end_date)) DESC,
             global_year.start_date DESC
    LIMIT 1
)
UPDATE global_academic_years global_year
SET status = 'ACTIVE', activated_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
FROM candidate
WHERE global_year.id = candidate.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_global_academic_year_active
    ON global_academic_years ((status)) WHERE status = 'ACTIVE';
