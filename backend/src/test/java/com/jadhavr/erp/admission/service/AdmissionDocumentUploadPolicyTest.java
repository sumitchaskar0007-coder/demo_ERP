package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AdmissionDocumentUploadPolicyTest {
    @Test
    void rejectsWebpDocuments() {
        assertThrows(BadRequestException.class, () -> AdmissionDocumentUploadPolicy.validate(
                "marksheet.webp",
                "image/webp",
                1024,
                "a".repeat(64),
                2L * 1024 * 1024));
    }
}
