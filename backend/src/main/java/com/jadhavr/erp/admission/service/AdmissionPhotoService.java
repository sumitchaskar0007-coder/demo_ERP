package com.jadhavr.erp.admission.service;

import com.jadhavr.erp.admission.entity.AdmissionForm;
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
import java.util.UUID;

@Service
public class AdmissionPhotoService {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");

    private final AdmissionFormRepository admissions;
    private final Path root;

    public AdmissionPhotoService(
            AdmissionFormRepository admissions,
            @Value("${app.storage.admission-photo-dir:uploads/admission-photos}") String directory) {
        this.admissions = admissions;
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public AdmissionForm save(Long admissionId, MultipartFile file) {
        AdmissionForm admission = findScoped(admissionId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Passport-size photo is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Photo must not exceed 2 MB");
        }
        if (admission.getStatus() != com.jadhavr.erp.admission.enums.AdmissionStatus.STUDENT_DETAILS_PENDING
                && admission.getStatus() != com.jadhavr.erp.admission.enums.AdmissionStatus.SUBMITTED
                && admission.getStatus() != com.jadhavr.erp.admission.enums.AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING
                && admission.getStatus() != com.jadhavr.erp.admission.enums.AdmissionStatus.STUDENT_SECTION_REJECTED) {
            throw new BadRequestException("Student photo cannot be changed after Student Section approval");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPEG, PNG, or WebP photos are allowed");
        }
        try {
            Files.createDirectories(root);
            String oldName = admission.getPhotoStorageName();
            String storageName = admission.getId() + "-" + UUID.randomUUID() + extension;
            Path target = safePath(storageName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            admission.setPhotoStorageName(storageName);
            admission.setPhotoVerified(false);
            AdmissionForm saved = admissions.save(admission);
            if (oldName != null) Files.deleteIfExists(safePath(oldName));
            return saved;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to store the student photo");
        }
    }

    @Transactional(readOnly = true)
    public PhotoResource load(Long admissionId) {
        AdmissionForm admission = findScoped(admissionId);
        if (admission.getPhotoStorageName() == null) {
            throw new ResourceNotFoundException("Student photo not uploaded");
        }
        try {
            Path file = safePath(admission.getPhotoStorageName());
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Student photo not found");
            }
            String contentType = Files.probeContentType(file);
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            return new PhotoResource(resource, mediaType);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Student photo not found");
        }
    }

    private AdmissionForm findScoped(Long id) {
        AdmissionForm admission = admissions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (!SecurityUtils.isSuperAdmin()
                && !admission.getCollege().getId().equals(SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
        return admission;
    }

    private Path safePath(String storageName) {
        Path path = root.resolve(storageName).normalize();
        if (!path.startsWith(root)) throw new BadRequestException("Invalid photo path");
        return path;
    }

    public record PhotoResource(Resource resource, MediaType mediaType) {}
}
