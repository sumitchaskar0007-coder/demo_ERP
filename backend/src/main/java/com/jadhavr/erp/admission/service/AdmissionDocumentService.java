package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.entity.AdmissionDocument;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionDocumentType;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.BadRequestException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import com.jadhavr.erp.storage.ObjectStorageService;
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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdmissionDocumentService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionDocumentService.class);
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");
    private static final Set<AdmissionStatus> EDITABLE = Set.of(
            AdmissionStatus.SUBMITTED,
            AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING,
            AdmissionStatus.STUDENT_SECTION_REJECTED,
            AdmissionStatus.PRINCIPAL_REJECTED);

    private final AdmissionFormRepository admissions;
    private final AdmissionDocumentRepository documents;
    private final ObjectStorageService storage;
    private AdmissionDocumentRequirementService requirements;

    public AdmissionDocumentService(AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            ObjectStorageService storage) {
        this.admissions = admissions;
        this.documents = documents;
        this.storage = storage;
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
        if (file == null || file.isEmpty()) throw new BadRequestException("Document file is required");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Document must not exceed 2 MB");
        String contentType = resolveContentType(file);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) throw new BadRequestException("Only PDF, JPEG, PNG, or WebP documents are allowed");
        verifySignature(file, contentType);

        AdmissionDocument document = documents
                .findByAdmissionFormIdAndDocumentTypeForUpdate(admission.getId(), type)
                .orElseGet(AdmissionDocument::new);
        String oldStorageName = document.getStorageName();
        String storageName = "colleges/" + admission.getCollege().getId()
                + "/admissions/" + admission.getId()
                + "/documents/" + type.toLowerCase(Locale.ROOT)
                + "/" + UUID.randomUUID() + extension;
        try {
            storage.put(storageName, file.getBytes(), contentType);
            document.setAdmissionForm(admission);
            document.setDocumentType(type);
            document.setStorageName(storageName);
            document.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename(), type, extension));
            document.setContentType(contentType);
            document.setFileSize(file.getSize());
            document.setSha256Checksum(null);
            document.setVerifiedAt(null);
            AdmissionDocument saved = documents.saveAndFlush(document);
            registerObjectCleanup(storageName, oldStorageName);
            return saved;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the admission document");
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

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.trim().toLowerCase(Locale.ROOT);
            if (EXTENSIONS.containsKey(contentType)) return contentType;
        }
        String filename = file.getOriginalFilename();
        if (filename == null) return contentType;
        String lowercase = filename.toLowerCase(Locale.ROOT);
        if (lowercase.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (lowercase.endsWith(".jpg") || lowercase.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (lowercase.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lowercase.endsWith(".webp")) return "image/webp";
        return contentType;
    }

    private void verifySignature(MultipartFile file, String contentType) {
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            boolean valid = switch (contentType) {
                case MediaType.APPLICATION_PDF_VALUE -> header.length >= 5
                        && header[0] == '%' && header[1] == 'P' && header[2] == 'D'
                        && header[3] == 'F' && header[4] == '-';
                case MediaType.IMAGE_JPEG_VALUE -> header.length >= 3
                        && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                        && (header[2] & 0xff) == 0xff;
                case MediaType.IMAGE_PNG_VALUE -> header.length >= 8
                        && (header[0] & 0xff) == 0x89 && header[1] == 0x50
                        && header[2] == 0x4e && header[3] == 0x47;
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BadRequestException("Uploaded file content does not match its file type");
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the uploaded document");
        }
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
