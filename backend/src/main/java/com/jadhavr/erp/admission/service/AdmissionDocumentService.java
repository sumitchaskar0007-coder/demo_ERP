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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdmissionDocumentService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
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
    private final Path root;

    public AdmissionDocumentService(AdmissionFormRepository admissions,
            AdmissionDocumentRepository documents,
            @Value("${app.storage.admission-document-dir:uploads/admission-documents}") String directory) {
        this.admissions = admissions;
        this.documents = documents;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public AdmissionDocument saveMine(AdmissionDocumentType type, MultipartFile file) {
        AdmissionForm admission = admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return save(admission, type, file);
    }

    @Transactional
    public AdmissionDocument save(Long admissionId, AdmissionDocumentType type, MultipartFile file) {
        return save(findScoped(admissionId), type, file);
    }

    private AdmissionDocument save(AdmissionForm admission, AdmissionDocumentType type, MultipartFile file) {
        if (!EDITABLE.contains(admission.getStatus())) {
            throw new BadRequestException("Admission documents cannot be changed after approval");
        }
        if (file == null || file.isEmpty()) throw new BadRequestException("Document file is required");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Document must not exceed 5 MB");
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) throw new BadRequestException("Only PDF, JPEG, PNG, or WebP documents are allowed");
        verifySignature(file, contentType);

        AdmissionDocument document = documents
                .findByAdmissionFormIdAndDocumentType(admission.getId(), type)
                .orElseGet(AdmissionDocument::new);
        String oldStorageName = document.getStorageName();
        String storageName = admission.getId() + "-" + type.name().toLowerCase()
                + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), safePath(storageName), StandardCopyOption.REPLACE_EXISTING);
            document.setAdmissionForm(admission);
            document.setDocumentType(type);
            document.setStorageName(storageName);
            document.setOriginalFilename(safeOriginalFilename(file.getOriginalFilename(), type, extension));
            document.setContentType(contentType);
            document.setFileSize(file.getSize());
            AdmissionDocument saved = documents.save(document);
            if (oldStorageName != null) Files.deleteIfExists(safePath(oldStorageName));
            return saved;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the admission document");
        }
    }

    @Transactional(readOnly = true)
    public DocumentResource loadMine(AdmissionDocumentType type) {
        AdmissionForm admission = admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return load(admission, type);
    }

    @Transactional(readOnly = true)
    public DocumentResource load(Long admissionId, AdmissionDocumentType type) {
        return load(findScoped(admissionId), type);
    }

    private DocumentResource load(AdmissionForm admission, AdmissionDocumentType type) {
        AdmissionDocument document = documents.findByAdmissionFormIdAndDocumentType(admission.getId(), type)
                .orElseThrow(() -> new ResourceNotFoundException("Admission document not uploaded"));
        try {
            Resource resource = new UrlResource(safePath(document.getStorageName()).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Admission document not found");
            }
            return new DocumentResource(resource, MediaType.parseMediaType(document.getContentType()),
                    document.getOriginalFilename());
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Admission document not found");
        }
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

    private Path safePath(String storageName) {
        Path path = root.resolve(storageName).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid document path");
        return path;
    }

    private String safeOriginalFilename(String original, AdmissionDocumentType type, String extension) {
        if (original == null || original.isBlank()) return type.name().toLowerCase() + extension;
        String normalized = original.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return filename.isBlank() ? type.name().toLowerCase() + extension
                : filename.substring(0, Math.min(filename.length(), 255));
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

    public record DocumentResource(Resource resource, MediaType mediaType, String filename) {}
}
