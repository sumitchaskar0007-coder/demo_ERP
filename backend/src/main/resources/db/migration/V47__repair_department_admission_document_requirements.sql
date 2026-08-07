-- Repairs legacy or partially migrated installations and guarantees every department
-- starts with the canonical admission-document requirements.
ALTER TABLE admission_document_requirements
    ADD COLUMN IF NOT EXISTS department_id BIGINT;

CREATE TEMP TABLE legacy_department_requirement_repair ON COMMIT DROP AS
SELECT college_id, document_key, document_name, required, active, display_order,
       version, created_at, updated_at
FROM admission_document_requirements
WHERE department_id IS NULL;

DELETE FROM admission_document_requirements
WHERE department_id IS NULL;

ALTER TABLE admission_document_requirements
    DROP CONSTRAINT IF EXISTS uk_admission_document_requirement_key;
DROP INDEX IF EXISTS uk_admission_document_requirement_name;
DROP INDEX IF EXISTS idx_admission_document_requirement_active_order;

INSERT INTO admission_document_requirements
    (college_id, department_id, document_key, document_name, required, active,
     display_order, version, created_at, updated_at)
SELECT department.college_id, department.id, requirement.document_key,
       requirement.document_name, requirement.required, requirement.active,
       requirement.display_order, requirement.version,
       requirement.created_at, requirement.updated_at
FROM legacy_department_requirement_repair requirement
JOIN departments department ON department.college_id = requirement.college_id
WHERE NOT EXISTS (
    SELECT 1
    FROM admission_document_requirements existing
    WHERE existing.department_id = department.id
      AND existing.document_key = requirement.document_key
);

ALTER TABLE admission_document_requirements
    ALTER COLUMN department_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_admission_document_requirement_department'
          AND conrelid = 'admission_document_requirements'::regclass
    ) THEN
        ALTER TABLE admission_document_requirements
            ADD CONSTRAINT fk_admission_document_requirement_department
            FOREIGN KEY (department_id) REFERENCES departments(id);
    END IF;
END $$;

ALTER TABLE admission_document_requirements
    ADD CONSTRAINT uk_admission_document_requirement_key
    UNIQUE (department_id, document_key);

CREATE UNIQUE INDEX uk_admission_document_requirement_name
    ON admission_document_requirements(department_id, LOWER(document_name));

CREATE INDEX idx_admission_document_requirement_active_order
    ON admission_document_requirements(department_id, active, display_order, id);

INSERT INTO admission_document_requirements
    (college_id, department_id, document_key, document_name, required, active,
     display_order, version, created_at, updated_at)
SELECT department.college_id, department.id, defaults.document_key,
       defaults.document_name, defaults.required, TRUE, defaults.display_order,
       0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM departments department
CROSS JOIN (VALUES
    ('TENTH_MARKSHEET', '10th Marksheet', TRUE, 10),
    ('TWELFTH_MARKSHEET', '12th Marksheet', TRUE, 20),
    ('PROVISIONAL_CERTIFICATE', 'Provisional Certificate', TRUE, 30),
    ('TRANSFER_CERTIFICATE', 'Transfer Certificate', TRUE, 40),
    ('NATIONALITY_CERTIFICATE', 'Nationality Certificate', TRUE, 50),
    ('DOMICILE_CERTIFICATE', 'Domicile Certificate', TRUE, 60),
    ('AADHAAR_CARD', 'Aadhaar Card', TRUE, 70),
    ('GRADUATION_MARKSHEET', 'Graduation Marksheet', FALSE, 80),
    ('MIGRATION_CERTIFICATE', 'Migration Certificate', FALSE, 90),
    ('GAP_CERTIFICATE', 'Gap Certificate', FALSE, 100),
    ('ENTRANCE_SCORE_CARD', 'Entrance Score Card', FALSE, 110),
    ('CASTE_CERTIFICATE', 'Caste Certificate', FALSE, 120),
    ('CASTE_VALIDITY', 'Caste Validity', FALSE, 130),
    ('NON_CREAMY_LAYER_CERTIFICATE', 'Non-Creamy Layer Certificate', FALSE, 140),
    ('NAME_CHANGE_CERTIFICATE', 'Name Change Certificate', FALSE, 150),
    ('INCOME_CERTIFICATE', 'Income Certificate', FALSE, 160),
    ('FORM_O_MINORITY', 'Form O / Minority Certificate', FALSE, 170)
) AS defaults(document_key, document_name, required, display_order)
ON CONFLICT (department_id, document_key) DO NOTHING;
