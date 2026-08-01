ALTER TABLE admission_document_uploads
    DROP CONSTRAINT IF EXISTS ck_admission_document_upload_safe_key;

ALTER TABLE admission_document_uploads
    ADD CONSTRAINT ck_admission_document_upload_safe_key
        CHECK (
            (
                storage_name LIKE 'colleges/%/quarantine/admission-documents/%'
                OR storage_name LIKE 'colleges/%/admission-documents/%'
            )
            AND storage_name NOT LIKE '%..%'
            AND left(storage_name, 1) <> '/'
        );
