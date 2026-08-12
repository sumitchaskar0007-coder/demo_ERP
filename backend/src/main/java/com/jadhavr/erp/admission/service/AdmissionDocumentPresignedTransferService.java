package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.dto.AdmissionDocumentCompletionResponse;
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
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.storage.ObjectStorageService;
import com.jadhavr.erp.storage.PresignedObjectStorageService;
import com.jadhavr.erp.storage.SecureFileContentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("preprod | production")
public class AdmissionDocumentPresignedTransferService {
    private static final long MAX_DOCUMENT_BYTES = 2L * 1024 * 1024;
    private static final Logger log =
            LoggerFactory.getLogger(AdmissionDocumentPresignedTransferService.class);
    private static final Set<AdmissionStatus> EDITABLE = Set.of(
            AdmissionStatus.SUBMITTED,
            AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING,
            AdmissionStatus.STUDENT_SECTION_REJECTED,
            AdmissionStatus.PRINCIPAL_REJECTED);

    private final AdmissionFormRepository admissions;
    private final AdmissionDocumentRepository documents;
    private final AdmissionDocumentUploadRepository uploads;
    private final PresignedObjectStorageService presignedStorage;
    private final ObjectStorageService storage;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final long maxBytes;
    private final Duration uploadUrlExpiry;
    private final Duration downloadUrlExpiry;
    private final Duration pendingUploadExpiry;
    private final SecureFileContentValidator fileValidator;
    private final boolean malwareScanRequired;
    private AdmissionDocumentRequirementService requirements;

    @Autowired(required = false)
    public void setRequirements(AdmissionDocumentRequirementService requirements) {
        this.requirements = requirements;
    }

    @Autowired
    public AdmissionDocumentPresignedTransferService(
            AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            AdmissionDocumentUploadRepository uploads,
            PresignedObjectStorageService presignedStorage,
            ObjectStorageService storage,
            ApplicationEventPublisher events,
            @Value("${app.storage.presign.max-bytes:2097152}") long maxBytes,
            @Value("${app.storage.presign.upload-expiry-seconds:300}") long uploadExpirySeconds,
            @Value("${app.storage.presign.download-expiry-seconds:120}") long downloadExpirySeconds,
            @Value("${app.storage.presign.pending-expiry-seconds:900}") long pendingExpirySeconds,
            @Value("${app.storage.malware-scan.required:true}") boolean malwareScanRequired,
            SecureFileContentValidator fileValidator) {
        this(
                admissions,
                documents,
                uploads,
                presignedStorage,
                storage,
                events,
                Clock.systemUTC(),
                maxBytes,
                uploadExpirySeconds,
                downloadExpirySeconds,
                pendingExpirySeconds,
                malwareScanRequired,
                fileValidator);
    }

    AdmissionDocumentPresignedTransferService(
            AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            AdmissionDocumentUploadRepository uploads,
            PresignedObjectStorageService presignedStorage,
            ObjectStorageService storage,
            ApplicationEventPublisher events,
            Clock clock,
            long maxBytes,
            long uploadExpirySeconds,
            long downloadExpirySeconds,
            long pendingExpirySeconds) {
        this(admissions, documents, uploads, presignedStorage, storage, events, clock,
                maxBytes, uploadExpirySeconds, downloadExpirySeconds, pendingExpirySeconds,
                false, new SecureFileContentValidator());
    }

    AdmissionDocumentPresignedTransferService(
            AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            AdmissionDocumentUploadRepository uploads,
            PresignedObjectStorageService presignedStorage,
            ObjectStorageService storage,
            ApplicationEventPublisher events,
            Clock clock,
            long maxBytes,
            long uploadExpirySeconds,
            long downloadExpirySeconds,
            long pendingExpirySeconds,
            boolean malwareScanRequired,
            SecureFileContentValidator fileValidator) {
        long effectiveMaxBytes = Math.min(maxBytes, MAX_DOCUMENT_BYTES);
        AdmissionDocumentUploadPolicy.validateMaxBytes(effectiveMaxBytes);
        this.uploadUrlExpiry = validatedDuration(
                "app.storage.presign.upload-expiry-seconds", uploadExpirySeconds, 60, 900);
        this.downloadUrlExpiry = validatedDuration(
                "app.storage.presign.download-expiry-seconds", downloadExpirySeconds, 30, 900);
        this.pendingUploadExpiry = validatedDuration(
                "app.storage.presign.pending-expiry-seconds", pendingExpirySeconds, 60, 3600);
        if (this.pendingUploadExpiry.compareTo(this.uploadUrlExpiry) < 0) {
            throw new IllegalArgumentException(
                    "app.storage.presign.pending-expiry-seconds must not be shorter than the upload URL expiry");
        }
        this.admissions = admissions;
        this.documents = documents;
        this.uploads = uploads;
        this.presignedStorage = presignedStorage;
        this.storage = storage;
        this.events = events;
        this.clock = clock;
        this.maxBytes = effectiveMaxBytes;
        this.malwareScanRequired = malwareScanRequired;
        this.fileValidator = fileValidator;
    }

    @Transactional
    public AdmissionDocumentUploadResponse initiateMine(
            String type,
            AdmissionDocumentPresignRequest request) {
        return initiate(findMine(), type, request);
    }

    @Transactional
    public AdmissionDocumentUploadResponse initiate(
            Long admissionId,
            String type,
            AdmissionDocumentPresignRequest request) {
        return initiate(findScoped(admissionId), type, request);
    }

    private AdmissionDocumentUploadResponse initiate(
            AdmissionForm admission,
            String rawType,
            AdmissionDocumentPresignRequest request) {
        String type = AdmissionDocumentRequirementService.normalizeKey(rawType);
        requireEditable(admission);
        if (requirements != null) requirements.requireActive(admission, type);
        AdmissionDocumentUploadPolicy.ValidatedUpload validated =
                AdmissionDocumentUploadPolicy.validate(
                        request.originalFilename(),
                        request.contentType(),
                        request.fileSize(),
                        request.sha256(),
                        maxBytes);
        Long collegeId = requireCollegeId(admission);
        UUID uploadId = UUID.randomUUID();
        String storageName = quarantinePrefix(collegeId)
                + uploadId + validated.storageExtension();
        Instant now = clock.instant();
        Instant completionDeadline = now.plus(pendingUploadExpiry);

        AdmissionDocumentUpload upload = new AdmissionDocumentUpload();
        upload.setId(uploadId);
        upload.setAdmissionForm(admission);
        upload.setDocumentType(type);
        upload.setStorageName(storageName);
        upload.setOriginalFilename(validated.originalFilename());
        upload.setContentType(validated.contentType());
        upload.setExpectedFileSize(validated.fileSize());
        upload.setExpectedSha256(validated.sha256());
        upload.setExpectedSha256Base64(validated.checksumBase64());
        upload.setStatus(AdmissionDocumentUploadStatus.PENDING);
        upload.setExpiresAt(completionDeadline);
        uploads.saveAndFlush(upload);

        PresignedObjectStorageService.PresignedUpload presigned =
                presignedStorage.presignUpload(
                        storageName,
                        validated.contentType(),
                        validated.fileSize(),
                        validated.checksumBase64(),
                        uploadUrlExpiry);
        return new AdmissionDocumentUploadResponse(
                uploadId,
                presigned.url(),
                presigned.requiredHeaders(),
                presigned.expiresAt(),
                completionDeadline);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public AdmissionDocumentCompletionResponse completeMine(
            String type,
            UUID uploadId) {
        AdmissionForm mine = findMine();
        return complete(findScopedForUpdate(mine.getId()), type, uploadId);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public AdmissionDocumentCompletionResponse complete(
            Long admissionId,
            String type,
            UUID uploadId) {
        return complete(findScopedForUpdate(admissionId), type, uploadId);
    }

    private AdmissionDocumentCompletionResponse complete(
            AdmissionForm admission,
            String rawType,
            UUID uploadId) {
        String type = AdmissionDocumentRequirementService.normalizeKey(rawType);
        AdmissionDocumentUpload upload = uploads.findByIdForUpdate(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Document upload session not found"));
        requireMatchingSession(upload, admission, type);

        if (upload.getStatus() == AdmissionDocumentUploadStatus.COMPLETED) {
            AdmissionDocument existing = documents
                    .findByAdmissionFormIdAndDocumentTypeForUpdate(admission.getId(), type)
                    .filter(document -> document.getStorageName().equals(upload.getStorageName()))
                    .orElseThrow(() -> new BadRequestException(
                            "The completed upload is no longer the current document"));
            return completionResponse(existing);
        }
        requireEditable(admission);
        if (upload.getStatus() != AdmissionDocumentUploadStatus.PENDING) {
            throw new BadRequestException("Document upload session is no longer active");
        }

        Instant now = clock.instant();
        if (!now.isBefore(upload.getExpiresAt())) {
            expire(upload, "UPLOAD_SESSION_EXPIRED");
            throw new BadRequestException("Document upload session has expired");
        }
        Long collegeId = requireCollegeId(admission);
        requireQuarantineStorageKey(upload.getStorageName(), collegeId);

        PresignedObjectStorageService.ObjectMetadata metadata =
                presignedStorage.head(upload.getStorageName());
        String failureCode = verificationFailure(upload, metadata);
        if (failureCode != null) {
            quarantine(upload, failureCode);
            throw new BadRequestException("Uploaded document failed integrity verification");
        }
        if (malwareScanRequired) {
            String scanStatus = metadata.malwareScanStatus();
            if (scanStatus == null || scanStatus.isBlank()) {
                throw new UploadScanPendingException();
            }
            if (!"NO_THREATS_FOUND".equals(scanStatus)) {
                quarantine(upload, "MALWARE_SCAN_" + scanStatus);
                throw new BadRequestException("Uploaded document failed malware screening");
            }
        }

        SecureFileContentValidator.ValidatedContent validated;
        try {
            ObjectStorageService.StoredObject quarantined = storage.get(upload.getStorageName());
            validated = fileValidator.validateDocument(
                    quarantined.content(), upload.getContentType(), maxBytes);
        } catch (BadRequestException exception) {
            quarantine(upload, "CONTENT_VALIDATION_FAILED");
            throw exception;
        }
        String quarantineStorageName = upload.getStorageName();
        String approvedStorageName = tenantPrefix(collegeId)
                + upload.getId() + validated.extension();
        storage.put(approvedStorageName, validated.content(), validated.contentType());

        AdmissionDocument document = documents
                .findByAdmissionFormIdAndDocumentTypeForUpdate(admission.getId(), type)
                .orElseGet(AdmissionDocument::new);
        String oldStorageName = document.getStorageName();
        document.setAdmissionForm(admission);
        document.setDocumentType(type);
        document.setStorageName(approvedStorageName);
        document.setOriginalFilename(upload.getOriginalFilename());
        document.setContentType(validated.contentType());
        document.setFileSize((long) validated.content().length);
        document.setSha256Checksum(validated.sha256());
        document.setVerifiedAt(now);
        AdmissionDocument saved = documents.saveAndFlush(document);

        upload.setStorageName(approvedStorageName);
        upload.setStatus(AdmissionDocumentUploadStatus.COMPLETED);
        upload.setCompletedAt(now);
        upload.setFailureCode(null);
        uploads.save(upload);
        registerPromotionCleanup(
                approvedStorageName, quarantineStorageName, oldStorageName, upload);
        return completionResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdmissionDocumentDownloadUrlResponse downloadMine(String type) {
        return download(findMine(), type);
    }

    @Transactional(readOnly = true)
    public AdmissionDocumentDownloadUrlResponse download(
            Long admissionId,
            String type) {
        return download(findScoped(admissionId), type);
    }

    private AdmissionDocumentDownloadUrlResponse download(
            AdmissionForm admission,
            String rawType) {
        String type = AdmissionDocumentRequirementService.normalizeKey(rawType);
        AdmissionDocument document = documents
                .findByAdmissionFormIdAndDocumentType(admission.getId(), type)
                .orElseThrow(() -> new ResourceNotFoundException("Admission document not uploaded"));
        if (document.getSha256Checksum() == null || document.getVerifiedAt() == null) {
            throw new BadRequestException(
                    "This document must be downloaded through the existing document endpoint");
        }
        requireTenantStorageKey(document.getStorageName(), requireCollegeId(admission));
        PresignedObjectStorageService.PresignedDownload presigned =
                presignedStorage.presignDownload(
                        document.getStorageName(),
                        document.getOriginalFilename(),
                        document.getContentType(),
                        downloadUrlExpiry);
        return new AdmissionDocumentDownloadUrlResponse(
                presigned.url(),
                presigned.expiresAt(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getSha256Checksum());
    }

    @Scheduled(
            fixedDelayString = "${app.storage.presign.cleanup-delay-ms:300000}",
            initialDelayString = "${app.storage.presign.cleanup-initial-delay-ms:60000}")
    @Transactional
    public void expireAbandonedUploads() {
        List<AdmissionDocumentUpload> expired =
                uploads.findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        AdmissionDocumentUploadStatus.PENDING,
                        clock.instant());
        for (AdmissionDocumentUpload upload : expired) {
            expire(upload, "ABANDONED_UPLOAD_EXPIRED");
        }
    }

    private AdmissionForm findMine() {
        return admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
    }

    private AdmissionForm findScoped(Long admissionId) {
        AdmissionForm admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        requireScope(admission);
        return admission;
    }

    private AdmissionForm findScopedForUpdate(Long admissionId) {
        AdmissionForm admission = admissions.findByIdForUpdate(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        requireScope(admission);
        return admission;
    }

    private void requireScope(AdmissionForm admission) {
        if (SecurityUtils.hasRole("STUDENT")) {
            if (!admission.getStudentUser().getId().equals(SecurityUtils.getCurrentUserId())) {
                throw new AccessDeniedException("Admission does not belong to the current student");
            }
        } else if (!SecurityUtils.isSuperAdmin()
                && !admission.getCollege().getId().equals(
                        SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
    }

    private void requireMatchingSession(
            AdmissionDocumentUpload upload,
            AdmissionForm admission,
            String type) {
        if (!Objects.equals(upload.getAdmissionForm().getId(), admission.getId())
                || !Objects.equals(upload.getDocumentType(), type)) {
            throw new ResourceNotFoundException("Document upload session not found");
        }
    }

    private void requireEditable(AdmissionForm admission) {
        if (!EDITABLE.contains(admission.getStatus())) {
            throw new BadRequestException("Admission documents cannot be changed after approval");
        }
    }

    private Long requireCollegeId(AdmissionForm admission) {
        if (admission.getCollege() == null || admission.getCollege().getId() == null) {
            throw new BadRequestException("Admission college is invalid");
        }
        return admission.getCollege().getId();
    }

    private String verificationFailure(
            AdmissionDocumentUpload upload,
            PresignedObjectStorageService.ObjectMetadata metadata) {
        if (upload.getExpectedFileSize() == null
                || metadata.contentLength() != upload.getExpectedFileSize()) {
            return "SIZE_MISMATCH";
        }
        if (!upload.getContentType().equals(metadata.contentType())) return "CONTENT_TYPE_MISMATCH";
        if (metadata.checksumSha256() == null || metadata.checksumSha256().isBlank()) {
            return "CHECKSUM_MISSING";
        }
        if (!upload.getExpectedSha256Base64().equals(metadata.checksumSha256())) {
            return "CHECKSUM_MISMATCH";
        }
        return null;
    }

    private void quarantine(AdmissionDocumentUpload upload, String failureCode) {
        upload.setStatus(AdmissionDocumentUploadStatus.QUARANTINED);
        upload.setFailureCode(failureCode);
        uploads.save(upload);
        registerAfterCommit(() -> publishState(upload, failureCode));
    }

    private void expire(AdmissionDocumentUpload upload, String failureCode) {
        upload.setStatus(AdmissionDocumentUploadStatus.EXPIRED);
        upload.setFailureCode(failureCode);
        uploads.save(upload);
        registerAfterCommit(() -> {
            safeDelete(upload.getStorageName());
            publishState(upload, failureCode);
        });
    }

    private AdmissionDocumentCompletionResponse completionResponse(AdmissionDocument document) {
        return new AdmissionDocumentCompletionResponse(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getSha256Checksum(),
                document.getVerifiedAt());
    }

    public AdmissionDocumentUploadResponse initiateMine(
            AdmissionDocumentType type, AdmissionDocumentPresignRequest request) {
        return initiateMine(type.name(), request);
    }

    public AdmissionDocumentUploadResponse initiate(
            Long admissionId, AdmissionDocumentType type,
            AdmissionDocumentPresignRequest request) {
        return initiate(admissionId, type.name(), request);
    }

    public AdmissionDocumentCompletionResponse completeMine(
            AdmissionDocumentType type, UUID uploadId) {
        return completeMine(type.name(), uploadId);
    }

    public AdmissionDocumentCompletionResponse complete(
            Long admissionId, AdmissionDocumentType type, UUID uploadId) {
        return complete(admissionId, type.name(), uploadId);
    }

    public AdmissionDocumentDownloadUrlResponse downloadMine(AdmissionDocumentType type) {
        return downloadMine(type.name());
    }

    public AdmissionDocumentDownloadUrlResponse download(
            Long admissionId, AdmissionDocumentType type) {
        return download(admissionId, type.name());
    }

    private void publishState(AdmissionDocumentUpload upload, String reasonCode) {
        try {
            events.publishEvent(new AdmissionDocumentUploadStateChangedEvent(
                    upload.getId(),
                    upload.getAdmissionForm().getCollege().getId(),
                    upload.getStorageName(),
                    upload.getStatus(),
                    reasonCode));
        } catch (RuntimeException exception) {
            log.warn("Unable to publish admission-document upload state event", exception);
        }
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void registerPromotionCleanup(
            String approvedKey,
            String quarantineKey,
            String oldKey,
            AdmissionDocumentUpload upload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(quarantineKey);
            if (oldKey != null && !oldKey.equals(approvedKey)) safeDelete(oldKey);
            publishState(upload, null);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(quarantineKey);
                if (oldKey != null && !oldKey.equals(approvedKey)) safeDelete(oldKey);
                publishState(upload, null);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(approvedKey);
            }
        });
    }

    private void safeDelete(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException exception) {
            log.warn("Unable to clean up an admission-document upload object", exception);
        }
    }

    private static Duration validatedDuration(
            String property,
            long seconds,
            long minimum,
            long maximum) {
        if (seconds < minimum || seconds > maximum) {
            throw new IllegalArgumentException(
                    property + " must be between " + minimum + " and " + maximum + " seconds");
        }
        return Duration.ofSeconds(seconds);
    }

    private static String tenantPrefix(Long collegeId) {
        return "colleges/" + collegeId + "/admission-documents/";
    }

    private static String quarantinePrefix(Long collegeId) {
        return "colleges/" + collegeId + "/quarantine/admission-documents/";
    }

    private static void requireTenantStorageKey(String storageKey, Long collegeId) {
        if (storageKey == null || !storageKey.startsWith(tenantPrefix(collegeId))) {
            throw new BadRequestException("Admission document storage key is invalid");
        }
    }

    private static void requireQuarantineStorageKey(String storageKey, Long collegeId) {
        if (storageKey == null || !storageKey.startsWith(quarantinePrefix(collegeId))) {
            throw new BadRequestException("Admission document storage key is invalid");
        }
    }
}
