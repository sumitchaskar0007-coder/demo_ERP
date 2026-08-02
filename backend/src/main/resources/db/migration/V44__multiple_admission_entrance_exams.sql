CREATE TABLE admission_entrance_exams (
    admission_form_id BIGINT NOT NULL,
    exam_order INTEGER NOT NULL,
    exam_name VARCHAR(120) NOT NULL,
    result VARCHAR(100) NOT NULL,
    CONSTRAINT pk_admission_entrance_exams PRIMARY KEY (admission_form_id, exam_order),
    CONSTRAINT fk_admission_entrance_exams_form FOREIGN KEY (admission_form_id)
        REFERENCES admission_forms(id) ON DELETE CASCADE,
    CONSTRAINT chk_admission_entrance_exams_name CHECK (length(trim(exam_name)) > 0),
    CONSTRAINT chk_admission_entrance_exams_result CHECK (length(trim(result)) > 0)
);

INSERT INTO admission_entrance_exams (admission_form_id, exam_order, exam_name, result)
SELECT id,
       0,
       COALESCE(NULLIF(trim(qualifying_entrance_seat_number), ''), 'Qualifying entrance test'),
       COALESCE(qualifying_entrance_total_score::text, 'Not specified')
FROM admission_forms
WHERE NULLIF(trim(qualifying_entrance_seat_number), '') IS NOT NULL
   OR qualifying_entrance_total_score IS NOT NULL;

CREATE INDEX idx_admission_entrance_exams_form
    ON admission_entrance_exams(admission_form_id);
