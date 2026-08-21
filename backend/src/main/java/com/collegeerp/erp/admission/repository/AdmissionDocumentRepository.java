package com.collegeerp.erp.admission.repository;

import com.collegeerp.erp.admission.entity.AdmissionDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, Long> {
    Optional<AdmissionDocument> findByAdmissionFormIdAndDocumentType(Long admissionId, String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select document
            from AdmissionDocument document
            where document.admissionForm.id = :admissionId
              and document.documentType = :type
            """)
    Optional<AdmissionDocument> findByAdmissionFormIdAndDocumentTypeForUpdate(
            @Param("admissionId") Long admissionId,
            @Param("type") String type);

    @Query("select d.documentType from AdmissionDocument d where d.admissionForm.id = :admissionId")
    Set<String> findTypesByAdmissionId(@Param("admissionId") Long admissionId);

    List<AdmissionDocument> findByAdmissionFormIdOrderByDocumentType(Long admissionId);
}
