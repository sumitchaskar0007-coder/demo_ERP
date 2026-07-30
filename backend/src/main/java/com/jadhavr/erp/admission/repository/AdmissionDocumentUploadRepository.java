package com.jadhavr.erp.admission.repository;

import com.jadhavr.erp.admission.entity.AdmissionDocumentUpload;
import com.jadhavr.erp.admission.enums.AdmissionDocumentUploadStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionDocumentUploadRepository extends JpaRepository<AdmissionDocumentUpload, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select upload from AdmissionDocumentUpload upload where upload.id = :id")
    Optional<AdmissionDocumentUpload> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AdmissionDocumentUpload> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            AdmissionDocumentUploadStatus status,
            Instant expiresAt);
}
