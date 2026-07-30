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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesDocumentInTenantScopedObjectStorageWithLocaleSafeKey() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                    11L, AdmissionDocumentType.MIGRATION_CERTIFICATE.name()))
                    .thenReturn(Optional.empty());
            when(documents.saveAndFlush(any(AdmissionDocument.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdmissionDocument result = service.save(
                    11L,
                    AdmissionDocumentType.MIGRATION_CERTIFICATE,
                    pdf());

            assertTrue(result.getStorageName().matches(
                    "^colleges/7/admissions/11/documents/migration_certificate/[0-9a-f-]+\\.pdf$"));
            assertFalse(result.getStorageName().contains("ı"));
            verify(storage).put(
                    org.mockito.ArgumentMatchers.eq(result.getStorageName()),
                    any(byte[].class),
                    org.mockito.ArgumentMatchers.eq(MediaType.APPLICATION_PDF_VALUE));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void acceptsPdfWhenBrowserDoesNotProvideContentType() {
        when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                11L, AdmissionDocumentType.TENTH_MARKSHEET.name()))
                .thenReturn(Optional.empty());
        when(documents.saveAndFlush(any(AdmissionDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "document",
                "marksheet.pdf",
                "",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));

        AdmissionDocument saved =
                service.save(11L, AdmissionDocumentType.TENTH_MARKSHEET, file);

        assertEquals(MediaType.APPLICATION_PDF_VALUE, saved.getContentType());
        verify(storage).put(
                org.mockito.ArgumentMatchers.eq(saved.getStorageName()),
                any(byte[].class),
                org.mockito.ArgumentMatchers.eq(MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void rejectsDocumentLargerThanTwoMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "document",
                "large.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[2 * 1024 * 1024 + 1]);

        assertThrows(com.jadhavr.erp.common.exception.BadRequestException.class,
                () -> service.save(11L, "CUSTOM_LARGE_FILE", file));
        verify(storage, never()).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void deletesOldObjectOnlyAfterTransactionCommit() {
        AdmissionDocument existing = new AdmissionDocument();
        existing.setStorageName("old/document.pdf");
        when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                11L, AdmissionDocumentType.TENTH_MARKSHEET.name()))
                .thenReturn(Optional.of(existing));
        when(documents.saveAndFlush(existing)).thenReturn(existing);
        TransactionSynchronizationManager.initSynchronization();

        AdmissionDocument saved = service.save(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, pdf());

        verify(storage, never()).delete(anyString());
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.get(0).afterCommit();
        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage).delete("old/document.pdf");
        verify(storage, never()).delete(saved.getStorageName());
    }

    @Test
    void deletesNewObjectAndKeepsOldObjectWhenTransactionRollsBack() {
        AdmissionDocument existing = new AdmissionDocument();
        existing.setStorageName("old/document.pdf");
        when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                11L, AdmissionDocumentType.TENTH_MARKSHEET.name()))
                .thenReturn(Optional.of(existing));
        when(documents.saveAndFlush(existing)).thenReturn(existing);
        TransactionSynchronizationManager.initSynchronization();

        AdmissionDocument saved = service.save(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, pdf());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(saved.getStorageName());
        verify(storage, never()).delete("old/document.pdf");
    }

    @Test
    void deletesNewObjectWhenDatabaseSaveFails() {
        when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                11L, AdmissionDocumentType.TENTH_MARKSHEET.name()))
                .thenReturn(Optional.empty());
        when(documents.saveAndFlush(any(AdmissionDocument.class)))
                .thenThrow(new IllegalStateException("database write failed"));
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);

        assertThrows(IllegalStateException.class,
                () -> service.save(11L, AdmissionDocumentType.TENTH_MARKSHEET, pdf()));

        verify(storage).put(
                key.capture(),
                any(byte[].class),
                org.mockito.ArgumentMatchers.eq(MediaType.APPLICATION_PDF_VALUE));
        verify(storage).delete(key.getValue());
    }

    @Test
    void loadsBytesAndUsesDatabaseContentTypeForMigratedObject() throws Exception {
        AdmissionDocument document = new AdmissionDocument();
        document.setStorageName("legacy-document.pdf");
        document.setOriginalFilename("marksheet.pdf");
        document.setContentType(MediaType.APPLICATION_PDF_VALUE);
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET.name()))
                .thenReturn(Optional.of(document));
        byte[] content = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        when(storage.get("legacy-document.pdf"))
                .thenReturn(new ObjectStorageService.StoredObject(
                        content, MediaType.APPLICATION_OCTET_STREAM_VALUE));

        AdmissionDocumentService.DocumentResource result =
                service.load(11L, AdmissionDocumentType.TENTH_MARKSHEET);

        assertEquals(MediaType.APPLICATION_PDF, result.mediaType());
        assertEquals("marksheet.pdf", result.filename());
        assertArrayEquals(content, result.resource().getInputStream().readAllBytes());
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile(
                "document",
                "document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));
    }
}
