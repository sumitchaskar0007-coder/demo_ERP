package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.entity.AdmissionDocument;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.storage.ObjectStorageService;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentServiceTest {
    @Mock private AdmissionFormRepository admissions;
    @Mock private AdmissionDocumentRepository documents;
    @Mock private ObjectStorageService storage;

    private AdmissionDocumentService service;
    private AdmissionForm admission;

    @BeforeEach
    void setUp() {
        service = new AdmissionDocumentService(admissions, documents, storage);
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.SUPER_ADMIN, 1L, null));
        College college = new College();
        college.setId(7L);
        admission = new AdmissionForm();
        admission.setId(11L);
        admission.setCollege(college);
        admission.setStatus(AdmissionStatus.SUBMITTED);
        when(admissions.findById(11L)).thenReturn(Optional.of(admission));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesDocumentInTenantScopedObjectStorage() {
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.empty());
        when(documents.saveAndFlush(any(AdmissionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionDocument saved = service.save(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, pdf("application/pdf"));

        assertTrue(saved.getStorageName().matches(
                "^colleges/7/admissions/11/documents/tenth_marksheet/[0-9a-f-]+\\.pdf$"));
        verify(storage).put(
                org.mockito.ArgumentMatchers.eq(saved.getStorageName()),
                any(byte[].class),
                org.mockito.ArgumentMatchers.eq(MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void acceptsPdfWhenBrowserDoesNotProvideContentType() {
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.empty());
        when(documents.saveAndFlush(any(AdmissionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionDocument saved = service.save(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, pdf(""));

        assertEquals(MediaType.APPLICATION_PDF_VALUE, saved.getContentType());
    }

    @Test
    void loadsDocumentFromObjectStorage() throws Exception {
        AdmissionDocument document = new AdmissionDocument();
        document.setStorageName("colleges/7/admissions/11/documents/test.pdf");
        document.setOriginalFilename("marksheet.pdf");
        document.setContentType(MediaType.APPLICATION_PDF_VALUE);
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.of(document));
        byte[] content = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        when(storage.get(document.getStorageName()))
                .thenReturn(new ObjectStorageService.StoredObject(
                        content, MediaType.APPLICATION_PDF_VALUE));

        AdmissionDocumentService.DocumentResource result =
                service.load(11L, AdmissionDocumentType.TENTH_MARKSHEET);

        assertArrayEquals(content, result.resource().getInputStream().readAllBytes());
        assertEquals("marksheet.pdf", result.filename());
    }

    private MockMultipartFile pdf(String contentType) {
        return new MockMultipartFile(
                "file",
                "marksheet.pdf",
                contentType,
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));
    }
}
