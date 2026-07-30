package com.jadhavr.erp.admission.entity;

import com.jadhavr.erp.admission.enums.AdmissionDocumentUploadStatus;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admission_document_uploads")
public class AdmissionDocumentUpload extends BaseAuditEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_form_id", nullable = false)
    private AdmissionForm admissionForm;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "storage_name", nullable = false, unique = true, length = 220)
    private String storageName;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "expected_file_size", nullable = false)
    private Long expectedFileSize;

    @Column(name = "expected_sha256", nullable = false, length = 64)
    private String expectedSha256;

    @Column(name = "expected_sha256_base64", nullable = false, length = 44)
    private String expectedSha256Base64;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdmissionDocumentUploadStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AdmissionForm getAdmissionForm() { return admissionForm; }
    public void setAdmissionForm(AdmissionForm admissionForm) { this.admissionForm = admissionForm; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public void setDocumentType(com.jadhavr.erp.admission.enums.AdmissionDocumentType documentType) {
        this.documentType = documentType.name();
    }
    public String getStorageName() { return storageName; }
    public void setStorageName(String storageName) { this.storageName = storageName; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getExpectedFileSize() { return expectedFileSize; }
    public void setExpectedFileSize(Long expectedFileSize) { this.expectedFileSize = expectedFileSize; }
    public String getExpectedSha256() { return expectedSha256; }
    public void setExpectedSha256(String expectedSha256) { this.expectedSha256 = expectedSha256; }
    public String getExpectedSha256Base64() { return expectedSha256Base64; }
    public void setExpectedSha256Base64(String expectedSha256Base64) {
        this.expectedSha256Base64 = expectedSha256Base64;
    }
    public AdmissionDocumentUploadStatus getStatus() { return status; }
    public void setStatus(AdmissionDocumentUploadStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
}
