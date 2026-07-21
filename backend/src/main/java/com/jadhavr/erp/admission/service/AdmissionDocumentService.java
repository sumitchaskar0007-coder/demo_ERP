package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
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
    public enum DocumentType {
        TENTH_MARKSHEET, TWELFTH_MARKSHEET, GRADUATION_PG_CERTIFICATE,
        LEAVING_CERTIFICATE, MIGRATION_CERTIFICATE, GAP_AFFIDAVIT,
        CASTE_CERTIFICATE, INCOME_PROOF, NAME_CHANGE_CERTIFICATE, AADHAAR_CARD
    }

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String,String> EXTENSIONS = Map.of(
            MediaType.APPLICATION_PDF_VALUE, ".pdf",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");
    private static final Set<AdmissionStatus> EDITABLE = Set.of(
            AdmissionStatus.STUDENT_DETAILS_PENDING,
            AdmissionStatus.SUBMITTED,
            AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING,
            AdmissionStatus.STUDENT_SECTION_REJECTED);

    private final AdmissionFormRepository admissions;
    private final Path root;

    public AdmissionDocumentService(AdmissionFormRepository admissions,
            @Value("${app.storage.admission-document-dir:uploads/admission-documents}") String directory) {
        this.admissions = admissions;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public AdmissionForm save(Long admissionId, DocumentType type, MultipartFile file) {
        AdmissionForm admission = findScoped(admissionId);
        if (!EDITABLE.contains(admission.getStatus())) {
            throw new BadRequestException("Admission documents cannot be changed after Student Section approval");
        }
        if (file == null || file.isEmpty()) throw new BadRequestException("Document file is required");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("Document must not exceed 5 MB");
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) throw new BadRequestException("Only PDF, JPEG, PNG, or WebP documents are allowed");
        try {
            Files.createDirectories(root);
            String oldName = storageName(admission, type);
            String storageName = admission.getId() + "-" + type.name().toLowerCase() + "-" + UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), safePath(storageName), StandardCopyOption.REPLACE_EXISTING);
            setStorageName(admission, type, storageName);
            resetVerification(admission, type);
            AdmissionForm saved = admissions.save(admission);
            if (oldName != null) Files.deleteIfExists(safePath(oldName));
            return saved;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the admission document");
        }
    }

    @Transactional(readOnly = true)
    public DocumentResource load(Long admissionId, DocumentType type) {
        AdmissionForm admission = findScoped(admissionId);
        String storageName = storageName(admission, type);
        if (storageName == null) throw new ResourceNotFoundException("Admission document not uploaded");
        try {
            Path file = safePath(storageName);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new ResourceNotFoundException("Admission document not found");
            String contentType = Files.probeContentType(file);
            return new DocumentResource(resource, contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType));
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Admission document not found");
        }
    }

    private AdmissionForm findScoped(Long id) {
        AdmissionForm admission = admissions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (!SecurityUtils.isSuperAdmin() && !admission.getCollege().getId().equals(SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
        return admission;
    }

    private String storageName(AdmissionForm admission, DocumentType type) {
        return switch (type) {
            case TENTH_MARKSHEET -> admission.getTenthMarksheetStorageName();
            case TWELFTH_MARKSHEET -> admission.getTwelfthMarksheetStorageName();
            case GRADUATION_PG_CERTIFICATE -> admission.getGraduationPgCertificateStorageName();
            case LEAVING_CERTIFICATE -> admission.getLeavingCertificateStorageName();
            case MIGRATION_CERTIFICATE -> admission.getMigrationCertificateStorageName();
            case GAP_AFFIDAVIT -> admission.getGapAffidavitStorageName();
            case CASTE_CERTIFICATE -> admission.getCasteCertificateStorageName();
            case INCOME_PROOF -> admission.getIncomeProofStorageName();
            case NAME_CHANGE_CERTIFICATE -> admission.getNameChangeCertificateStorageName();
            case AADHAAR_CARD -> admission.getAadhaarCardStorageName();
        };
    }
    private void setStorageName(AdmissionForm admission, DocumentType type, String value) {
        switch (type) {
            case TENTH_MARKSHEET -> admission.setTenthMarksheetStorageName(value);
            case TWELFTH_MARKSHEET -> admission.setTwelfthMarksheetStorageName(value);
            case GRADUATION_PG_CERTIFICATE -> admission.setGraduationPgCertificateStorageName(value);
            case LEAVING_CERTIFICATE -> admission.setLeavingCertificateStorageName(value);
            case MIGRATION_CERTIFICATE -> admission.setMigrationCertificateStorageName(value);
            case GAP_AFFIDAVIT -> admission.setGapAffidavitStorageName(value);
            case CASTE_CERTIFICATE -> admission.setCasteCertificateStorageName(value);
            case INCOME_PROOF -> admission.setIncomeProofStorageName(value);
            case NAME_CHANGE_CERTIFICATE -> admission.setNameChangeCertificateStorageName(value);
            case AADHAAR_CARD -> admission.setAadhaarCardStorageName(value);
        }
    }
    private void resetVerification(AdmissionForm admission, DocumentType type) {
        switch (type) {
            case TENTH_MARKSHEET -> admission.setTenthMarksheetVerified(false);
            case TWELFTH_MARKSHEET -> admission.setTwelfthMarksheetVerified(false);
            case LEAVING_CERTIFICATE -> admission.setLeavingCertificateVerified(false);
            case AADHAAR_CARD -> admission.setAadhaarCardVerified(false);
            case GRADUATION_PG_CERTIFICATE -> admission.setGraduationPgCertificateVerified(false);
            case MIGRATION_CERTIFICATE -> admission.setMigrationCertificateVerified(false);
            case GAP_AFFIDAVIT -> admission.setGapAffidavitVerified(false);
            case CASTE_CERTIFICATE -> admission.setCasteCertificateVerified(false);
            case INCOME_PROOF -> admission.setIncomeProofVerified(false);
            case NAME_CHANGE_CERTIFICATE -> admission.setNameChangeCertificateVerified(false);
        }
    }
    private Path safePath(String storageName) {
        Path path = root.resolve(storageName).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid document path");
        return path;
    }

    public record DocumentResource(Resource resource, MediaType mediaType) {}
}
