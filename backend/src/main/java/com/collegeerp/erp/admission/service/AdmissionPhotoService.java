package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.storage.ImageUploadValidator;
import com.collegeerp.erp.storage.ObjectStorageService;
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

import java.util.UUID;

@Service
public class AdmissionPhotoService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionPhotoService.class);
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private final AdmissionFormRepository admissions;
    private final ObjectStorageService storage;
    private final ImageUploadValidator images;

    public AdmissionPhotoService(
            AdmissionFormRepository admissions,
            ObjectStorageService storage,
            ImageUploadValidator images) {
        this.admissions = admissions;
        this.storage = storage;
        this.images = images;
    }

    @Transactional
    public AdmissionForm save(Long admissionId, MultipartFile file) {
        AdmissionForm admission = findScoped(admissionId);
        return save(admission, file);
    }

    @Transactional
    public AdmissionForm saveMine(MultipartFile file) {
        AdmissionForm admission = findMine();
        if (!studentCanEdit(admission)) {
            throw new BadRequestException("The admission form is read-only while it is pending or approved");
        }
        return save(admission, file);
    }

    private AdmissionForm save(AdmissionForm admission, MultipartFile file) {
        if (admission.getStatus() != com.collegeerp.erp.admission.enums.AdmissionStatus.SUBMITTED
                && admission.getStatus() != com.collegeerp.erp.admission.enums.AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING
                && admission.getStatus() != com.collegeerp.erp.admission.enums.AdmissionStatus.STUDENT_SECTION_REJECTED
                && admission.getStatus() != com.collegeerp.erp.admission.enums.AdmissionStatus.PRINCIPAL_REJECTED) {
            throw new BadRequestException("Student photo cannot be changed after Student Section approval");
        }
        ImageUploadValidator.ValidatedImage image = images.validate(file, MAX_BYTES);
        String oldKey = admission.getPhotoStorageName();
        String key = "colleges/" + admission.getCollege().getId()
                + "/admissions/" + admission.getId()
                + "/photo/" + UUID.randomUUID() + image.extension();
        storage.put(key, image.content(), image.contentType());
        try {
            admission.setPhotoStorageName(key);
            AdmissionForm saved = admissions.saveAndFlush(admission);
            registerObjectCleanup(key, oldKey);
            return saved;
        } catch (RuntimeException exception) {
            safeDelete(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PhotoResource load(Long admissionId) {
        AdmissionForm admission = findScoped(admissionId);
        return load(admission);
    }

    @Transactional(readOnly = true)
    public PhotoResource loadMine() {
        return load(findMine());
    }

    @Transactional
    public void delete(Long admissionId) {
        delete(findScoped(admissionId));
    }

    @Transactional
    public void deleteMine() {
        AdmissionForm admission = findMine();
        if (!studentCanEdit(admission)) {
            throw new BadRequestException("The admission form is read-only while it is pending or approved");
        }
        delete(admission);
    }

    private void delete(AdmissionForm admission) {
        String key = admission.getPhotoStorageName();
        if (key == null) return;
        admission.setPhotoStorageName(null);
        admissions.saveAndFlush(admission);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeDelete(key);
                }
            });
        } else {
            safeDelete(key);
        }
    }

    private PhotoResource load(AdmissionForm admission) {
        if (admission.getPhotoStorageName() == null) {
            throw new ResourceNotFoundException("Student photo not uploaded");
        }
        ObjectStorageService.StoredObject object = storage.get(admission.getPhotoStorageName());
        return new PhotoResource(new ByteArrayResource(object.content()),
                MediaType.parseMediaType(object.contentType()));
    }

    private AdmissionForm findScoped(Long id) {
        AdmissionForm admission = admissions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (SecurityUtils.hasRole("STUDENT")
                && !admission.getStudentUser().getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Admission does not belong to the current student");
        }
        if (!SecurityUtils.isSuperAdmin()
                && !admission.getCollege().getId().equals(SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
        return admission;
    }

    private AdmissionForm findMine() {
        return admissions.findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
    }

    private boolean studentCanEdit(AdmissionForm admission) {
        return (admission.getStatus() == com.collegeerp.erp.admission.enums.AdmissionStatus.SUBMITTED
                && admission.getDetailsCompletedAt() == null)
                || admission.getStatus() == com.collegeerp.erp.admission.enums.AdmissionStatus.STUDENT_SECTION_REJECTED
                || admission.getStatus() == com.collegeerp.erp.admission.enums.AdmissionStatus.PRINCIPAL_REJECTED;
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
            log.warn("Unable to complete object-storage cleanup", exception);
        }
    }

    public record PhotoResource(Resource resource, MediaType mediaType) {}
}
