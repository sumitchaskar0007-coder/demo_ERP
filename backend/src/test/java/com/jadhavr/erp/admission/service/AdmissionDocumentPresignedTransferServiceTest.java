package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDocumentDownloadUrlResponse;
import com.jadhavr.erp.admission.dto.AdmissionDocumentPresignRequest;
import com.jadhavr.erp.admission.dto.AdmissionDocumentUploadResponse;
import com.jadhavr.erp.admission.entity.AdmissionDocument;
import com.jadhavr.erp.admission.entity.AdmissionDocumentUpload;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.admission.enums.AdmissionDocumentUploadStatus;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import com.jadhavr.erp.admission.repository.AdmissionDocumentUploadRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.security.TestSecurityUsers;
import com.jadhavr.erp.storage.ObjectStorageService;
import com.jadhavr.erp.storage.PresignedObjectStorageService;
import com.jadhavr.erp.user.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentPresignedTransferServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");
    private static final String SHA256_HEX = "00".repeat(32);
    private static final String SHA256_BASE64 =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Mock private AdmissionFormRepository admissions;
    @Mock private AdmissionDocumentRepository documents;
    @Mock private AdmissionDocumentUploadRepository uploads;
    @Mock private PresignedObjectStorageService presignedStorage;
    @Mock private ObjectStorageService storage;
    @Mock private ApplicationEventPublisher events;

    private AdmissionDocumentPresignedTransferService service;
    private AdmissionForm admission;

    @BeforeEach
    void setUp() {
        service = new AdmissionDocumentPresignedTransferService(
                admissions,
                documents,
                uploads,
                presignedStorage,
                storage,
                events,
                Clock.fixed(NOW, ZoneOffset.UTC),
                5L * 1024 * 1024,
                300,
                120,
                900);
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.SUPER_ADMIN, 1L, null));

        College college = new College();
        college.setId(7L);
        admission = new AdmissionForm();
        admission.setId(11L);
        admission.setCollege(college);
        admission.setStatus(AdmissionStatus.SUBMITTED);
        org.mockito.Mockito.lenient().when(admissions.findById(11L))
                .thenReturn(Optional.of(admission));
        org.mockito.Mockito.lenient().when(admissions.findByIdForUpdate(11L))
                .thenReturn(Optional.of(admission));
    }

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsTenantOpaqueChecksumBoundUploadSession() {
        when(uploads.saveAndFlush(any(AdmissionDocumentUpload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(presignedStorage.presignUpload(
                anyString(), eq("application/pdf"), eq(1024L), eq(SHA256_BASE64), any()))
                .thenReturn(new PresignedObjectStorageService.PresignedUpload(
                        "https://uploads.example.test/signed",
                        NOW.plusSeconds(300),
                        Map.of("x-amz-checksum-sha256", List.of(SHA256_BASE64))));

        AdmissionDocumentUploadResponse result = service.initiate(
                11L,
                AdmissionDocumentType.TENTH_MARKSHEET,
                request("tenth-marksheet.pdf", "application/pdf", 1024));

        ArgumentCaptor<AdmissionDocumentUpload> upload =
                ArgumentCaptor.forClass(AdmissionDocumentUpload.class);
        verify(uploads).saveAndFlush(upload.capture());
        assertTrue(upload.getValue().getStorageName().matches(
                "^colleges/7/admission-documents/[0-9a-f-]+\\.pdf$"));
        assertFalse(upload.getValue().getStorageName().contains("tenth-marksheet"));
        assertFalse(upload.getValue().getStorageName().contains("/11/"));
        assertEquals(AdmissionDocumentUploadStatus.PENDING, upload.getValue().getStatus());
        assertEquals(SHA256_HEX, upload.getValue().getExpectedSha256());
        assertEquals(NOW.plusSeconds(900), upload.getValue().getExpiresAt());
        assertEquals(upload.getValue().getId(), result.uploadId());
        assertEquals(NOW.plusSeconds(300), result.uploadUrlExpiresAt());
        assertEquals(NOW.plusSeconds(900), result.completionDeadline());
    }

    @Test
    void rejectsTraversalMismatchedExtensionAndOversizedUploadBeforePresigning() {
        assertThrows(BadRequestException.class, () -> service.initiate(
                11L,
                AdmissionDocumentType.TENTH_MARKSHEET,
                request("../marksheet.pdf", "application/pdf", 1024)));
        assertThrows(BadRequestException.class, () -> service.initiate(
                11L,
                AdmissionDocumentType.TENTH_MARKSHEET,
                request("marksheet.png", "application/pdf", 1024)));
        assertThrows(BadRequestException.class, () -> service.initiate(
                11L,
                AdmissionDocumentType.TENTH_MARKSHEET,
                request("marksheet.pdf", "application/pdf", 5L * 1024 * 1024 + 1)));

        verify(uploads, never()).saveAndFlush(any());
        verify(presignedStorage, never()).presignUpload(
                anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong(),
                anyString(), any());
    }

    @Test
    void rejectsCrossCollegeInitiation() {
        SecurityContextHolder.getContext().setAuthentication(
                TestSecurityUsers.authentication(RoleName.STUDENT_SECTION, 22L, 8L));

        assertThrows(AccessDeniedException.class, () -> service.initiate(
                11L,
                AdmissionDocumentType.TENTH_MARKSHEET,
                request("marksheet.pdf", "application/pdf", 1024)));

        verify(uploads, never()).saveAndFlush(any());
    }

    @Test
    void headVerificationPromotesUploadAndDeletesOldObjectAfterCommit() {
        AdmissionDocumentUpload upload = pendingUpload(admission);
        AdmissionDocument oldDocument = new AdmissionDocument();
        oldDocument.setStorageName("colleges/7/admissions/11/documents/tenth/old.pdf");
        when(uploads.findByIdForUpdate(upload.getId())).thenReturn(Optional.of(upload));
        when(presignedStorage.head(upload.getStorageName()))
                .thenReturn(new PresignedObjectStorageService.ObjectMetadata(
                        1024, "application/pdf", SHA256_BASE64));
        when(documents.findByAdmissionFormIdAndDocumentTypeForUpdate(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.of(oldDocument));
        when(documents.saveAndFlush(oldDocument)).thenReturn(oldDocument);
        when(uploads.save(upload)).thenReturn(upload);
        TransactionSynchronizationManager.initSynchronization();

        var result = service.complete(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, upload.getId());

        assertEquals(AdmissionDocumentUploadStatus.COMPLETED, upload.getStatus());
        assertEquals(NOW, upload.getCompletedAt());
        assertEquals(SHA256_HEX, oldDocument.getSha256Checksum());
        assertEquals(NOW, oldDocument.getVerifiedAt());
        assertEquals(upload.getStorageName(), oldDocument.getStorageName());
        assertEquals(SHA256_HEX, result.sha256());
        verify(storage, never()).delete(anyString());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.get(0).afterCommit();
        verify(storage).delete("colleges/7/admissions/11/documents/tenth/old.pdf");
        verify(events).publishEvent(any(AdmissionDocumentUploadStateChangedEvent.class));
    }

    @Test
    void checksumMismatchQuarantinesWithoutPublishingDocument() {
        AdmissionDocumentUpload upload = pendingUpload(admission);
        when(uploads.findByIdForUpdate(upload.getId())).thenReturn(Optional.of(upload));
        when(presignedStorage.head(upload.getStorageName()))
                .thenReturn(new PresignedObjectStorageService.ObjectMetadata(
                        1024, "application/pdf", "not-the-expected-checksum"));
        when(uploads.save(upload)).thenReturn(upload);

        assertThrows(BadRequestException.class, () -> service.complete(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, upload.getId()));

        assertEquals(AdmissionDocumentUploadStatus.QUARANTINED, upload.getStatus());
        assertEquals("CHECKSUM_MISMATCH", upload.getFailureCode());
        verify(documents, never()).saveAndFlush(any());
        verify(storage, never()).delete(upload.getStorageName());
        verify(events).publishEvent(any(AdmissionDocumentUploadStateChangedEvent.class));
    }

    @Test
    void uploadSessionCannotBeCompletedForAnotherAdmission() {
        College otherCollege = new College();
        otherCollege.setId(8L);
        AdmissionForm otherAdmission = new AdmissionForm();
        otherAdmission.setId(12L);
        otherAdmission.setCollege(otherCollege);
        AdmissionDocumentUpload upload = pendingUpload(otherAdmission);
        when(uploads.findByIdForUpdate(upload.getId())).thenReturn(Optional.of(upload));

        assertThrows(ResourceNotFoundException.class, () -> service.complete(
                11L, AdmissionDocumentType.TENTH_MARKSHEET, upload.getId()));

        verify(presignedStorage, never()).head(anyString());
        verify(documents, never()).saveAndFlush(any());
    }

    @Test
    void createsShortLivedDownloadOnlyForVerifiedTenantDocument() {
        AdmissionDocument document = new AdmissionDocument();
        document.setAdmissionForm(admission);
        document.setDocumentType(AdmissionDocumentType.TENTH_MARKSHEET);
        document.setStorageName(
                "colleges/7/admission-documents/18fd6a4d-11ba-4a24-8313-00cad87c3f47.pdf");
        document.setOriginalFilename("marksheet.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(1024L);
        document.setSha256Checksum(SHA256_HEX);
        document.setVerifiedAt(NOW);
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.of(document));
        when(presignedStorage.presignDownload(
                document.getStorageName(), "marksheet.pdf", "application/pdf", java.time.Duration.ofSeconds(120)))
                .thenReturn(new PresignedObjectStorageService.PresignedDownload(
                        "https://downloads.example.test/signed", NOW.plusSeconds(120)));

        AdmissionDocumentDownloadUrlResponse result =
                service.download(11L, AdmissionDocumentType.TENTH_MARKSHEET);

        assertEquals("https://downloads.example.test/signed", result.downloadUrl());
        assertEquals(NOW.plusSeconds(120), result.expiresAt());
        assertEquals(SHA256_HEX, result.sha256());
    }

    @Test
    void expiresAndCleansAbandonedPendingUpload() {
        AdmissionDocumentUpload upload = pendingUpload(admission);
        upload.setExpiresAt(NOW.minusSeconds(1));
        when(uploads.findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                AdmissionDocumentUploadStatus.PENDING, NOW))
                .thenReturn(List.of(upload));
        when(uploads.save(upload)).thenReturn(upload);

        service.expireAbandonedUploads();

        assertEquals(AdmissionDocumentUploadStatus.EXPIRED, upload.getStatus());
        assertEquals("ABANDONED_UPLOAD_EXPIRED", upload.getFailureCode());
        verify(storage).delete(upload.getStorageName());
        verify(events).publishEvent(any(AdmissionDocumentUploadStateChangedEvent.class));
    }

    @Test
    void multipartDocumentCannotUsePresignedDownloadUntilVerified() {
        AdmissionDocument legacy = new AdmissionDocument();
        legacy.setStorageName("colleges/7/admissions/11/documents/tenth/legacy.pdf");
        legacy.setOriginalFilename("legacy.pdf");
        legacy.setContentType("application/pdf");
        legacy.setFileSize(1024L);
        when(documents.findByAdmissionFormIdAndDocumentType(
                11L, AdmissionDocumentType.TENTH_MARKSHEET))
                .thenReturn(Optional.of(legacy));

        assertThrows(BadRequestException.class,
                () -> service.download(11L, AdmissionDocumentType.TENTH_MARKSHEET));

        verify(presignedStorage, never()).presignDownload(
                anyString(), anyString(), anyString(), any());
    }

    private AdmissionDocumentPresignRequest request(
            String filename,
            String contentType,
            long size) {
        return new AdmissionDocumentPresignRequest(filename, contentType, size, SHA256_HEX);
    }

    private AdmissionDocumentUpload pendingUpload(AdmissionForm owner) {
        AdmissionDocumentUpload upload = new AdmissionDocumentUpload();
        upload.setId(UUID.randomUUID());
        upload.setAdmissionForm(owner);
        upload.setDocumentType(AdmissionDocumentType.TENTH_MARKSHEET);
        upload.setStorageName(
                "colleges/" + owner.getCollege().getId() + "/admission-documents/"
                        + upload.getId() + ".pdf");
        upload.setOriginalFilename("marksheet.pdf");
        upload.setContentType("application/pdf");
        upload.setExpectedFileSize(1024L);
        upload.setExpectedSha256(SHA256_HEX);
        upload.setExpectedSha256Base64(SHA256_BASE64);
        upload.setStatus(AdmissionDocumentUploadStatus.PENDING);
        upload.setExpiresAt(NOW.plusSeconds(900));
        upload.setFailureCode(null);
        upload.setCompletedAt(null);
        return upload;
    }
}
