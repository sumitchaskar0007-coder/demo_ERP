package com.jadhavr.erp.admission.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "admission_documents", uniqueConstraints =
        @UniqueConstraint(name = "uk_admission_document_type", columnNames = {"admission_form_id", "document_type"}))
public class AdmissionDocument extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_form_id", nullable = false)
    private AdmissionForm admissionForm;

    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "storage_name", nullable = false, length = 220)
    private String storageName;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256_checksum", length = 64)
    private String sha256Checksum;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    public Long getId() { return id; }
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
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
}
