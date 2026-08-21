package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.entity.AdmissionDocument;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.enums.AdmissionDocumentType;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.admission.repository.AdmissionDocumentRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.storage.ObjectStorageService;
import com.collegeerp.erp.storage.SecureFileContentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AdmissionDocumentService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionDocumentService.class);
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<AdmissionStatus> EDITABLE = Set.of(
            AdmissionStatus.SUBMITTED,
            AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING,
            AdmissionStatus.STUDENT_SECTION_REJECTED,
            AdmissionStatus.PRINCIPAL_REJECTED);

    private final AdmissionFormRepository admissions;
    private final AdmissionDocumentRepository documents;
    private final ObjectStorageService storage;
    private final SecureFileContentValidator fileValidator;
    private AdmissionDocumentRequirementService requirements;

    public AdmissionDocumentService(AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            ObjectStorageService storage) {
        this(admissions, documents, storage, new SecureFileContentValidator());
    }

    @Autowired
    public AdmissionDocumentService(AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            ObjectStorageService storage,
            SecureFileContentValidator fileValidator) {
        this.admissions = admissions;
        this.documents = documents;
        this.storage = storage;
        this.fileValidator = fileValidator;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRequirements(AdmissionDocumentRequirementService requirements) {
        this.requirements = requirements;
    }

    @Transactional
    public AdmissionDocument saveMine(String type, MultipartFile file) {
        AdmissionForm admission = admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return save(admission, type, file);
    }

    @Transactional
    public AdmissionDocument save(Long admissionId, String type, MultipartFile file) {
        return save(findScoped(admissionId), type, file);
    }

    private AdmissionDocument save(AdmissionForm admission, String rawType, MultipartFile file) {
        String type = AdmissionDocumentRequirementService.normalizeKey(rawType);
        if (requirements != null) requirements.requireActive(admission, type);
        if (!EDITABLE.contains(admission.getStatus())) {
            throw new BadRequestException("Admission documents cannot be changed after approval");
        }
        SecureFileContentValidator.ValidatedContent validated =
                fileValidator.validateDocument(file, MAX_BYTES);
        String contentType = validated.contentType();
        String extension = validated.extension();

        AdmissionDocument document = documents
                .findByAdmissionFormIdAndDocumentTypeForUpdate(admission.getId(), type)
                .orElseGet(AdmissionDocument::new);
        String oldStorageName = document.getStorageName();
        String storageName = "colleges/" + admission.getCollege().getId()
                + "/admissions/" + admission.getId()
                + "/documents/" + type.toLowerCase(Locale.ROOT)
                + "/" + UUID.randomUUID() + extension;
        try {
            storage.put(storageName, validated.content(), contentType);
            document.setAdmissionForm(admission);
            document.setDocumentType(type);
            document.setStorageName(storageName);
            document.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename(), type, extension));
            document.setContentType(contentType);
            document.setFileSize((long) validated.content().length);
            document.setSha256Checksum(null);
            document.setVerifiedAt(null);
            AdmissionDocument saved = documents.saveAndFlush(document);
            registerObjectCleanup(storageName, oldStorageName);
            return saved;
        } catch (RuntimeException exception) {
            safeDelete(storageName);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentResource loadMine(String type) {
        AdmissionForm admission = admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return load(admission, type);
    }

    @Transactional(readOnly = true)
    public DocumentResource load(Long admissionId, String type) {
        return load(findScoped(admissionId), type);
    }

    private DocumentResource load(AdmissionForm admission, String rawType) {
        String type = AdmissionDocumentRequirementService.normalizeKey(rawType);
        AdmissionDocument document = documents.findByAdmissionFormIdAndDocumentType(admission.getId(), type)
                .orElseThrow(() -> new ResourceNotFoundException("Admission document not uploaded"));
        ObjectStorageService.StoredObject object = storage.get(document.getStorageName());
        String contentType = document.getContentType();
        if (contentType == null || contentType.isBlank()) contentType = object.contentType();
        return new DocumentResource(
                new ByteArrayResource(object.content()),
                MediaType.parseMediaType(contentType),
                document.getOriginalFilename());
    }

    private AdmissionForm findScoped(Long admissionId) {
        AdmissionForm admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (SecurityUtils.hasRole("STUDENT")) {
            if (!admission.getStudentUser().getId().equals(SecurityUtils.getCurrentUserId())) {
                throw new AccessDeniedException("Admission does not belong to the current student");
            }
        } else if (!SecurityUtils.isSuperAdmin()
                && !admission.getCollege().getId().equals(SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
        return admission;
    }

    private String safeOriginalFilename(String original, String type, String extension) {
        if (original == null || original.isBlank()) return type.toLowerCase(Locale.ROOT) + extension;
        String normalized = original.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return filename.isBlank() ? type.toLowerCase(Locale.ROOT) + extension
                : filename.substring(0, Math.min(filename.length(), 255));
    }

    public AdmissionDocument saveMine(AdmissionDocumentType type, MultipartFile file) {
        return saveMine(type.name(), file);
    }

    public AdmissionDocument save(Long admissionId, AdmissionDocumentType type, MultipartFile file) {
        return save(admissionId, type.name(), file);
    }

    public DocumentResource loadMine(AdmissionDocumentType type) {
        return loadMine(type.name());
    }

    public DocumentResource load(Long admissionId, AdmissionDocumentType type) {
        return load(admissionId, type.name());
    }

    private void registerObjectCleanup(String newKey, String oldKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (oldKey != null) safeDelete(oldKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldKey != null) safeDelete(oldKey);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(newKey);
            }
        });
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Unable to complete admission-document storage cleanup", exception);
        }
    }

    public record DocumentResource(Resource resource, MediaType mediaType, String filename) {}
}
