ALTER TABLE admission_document_requirements
    ADD COLUMN department_id BIGINT;

CREATE TEMP TABLE legacy_admission_document_requirements AS
SELECT college_id, document_key, document_name, required, active, display_order
FROM admission_document_requirements;

DELETE FROM admission_document_requirements;

ALTER TABLE admission_document_requirements
    DROP CONSTRAINT uk_admission_document_requirement_key;
DROP INDEX uk_admission_document_requirement_name;
DROP INDEX idx_admission_document_requirement_active_order;

INSERT INTO admission_document_requirements
    (college_id, department_id, document_key, document_name, required, active, display_order)
SELECT department.college_id, department.id, requirement.document_key,
       requirement.document_name, requirement.required, requirement.active,
       requirement.display_order
FROM legacy_admission_document_requirements requirement
JOIN departments department ON department.college_id = requirement.college_id;

ALTER TABLE admission_document_requirements
    ALTER COLUMN department_id SET NOT NULL,
    ADD CONSTRAINT fk_admission_document_requirement_department
        FOREIGN KEY (department_id) REFERENCES departments(id),
    ADD CONSTRAINT uk_admission_document_requirement_key
        UNIQUE (department_id, document_key);

CREATE UNIQUE INDEX uk_admission_document_requirement_name
    ON admission_document_requirements(department_id, LOWER(document_name));

CREATE INDEX idx_admission_document_requirement_active_order
    ON admission_document_requirements(department_id, active, display_order, id);
