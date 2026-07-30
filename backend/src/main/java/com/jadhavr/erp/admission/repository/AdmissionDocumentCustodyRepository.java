package com.jadhavr.erp.admission.repository;
import com.jadhavr.erp.admission.entity.AdmissionDocumentCustody;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AdmissionDocumentCustodyRepository extends JpaRepository<AdmissionDocumentCustody,Long> {
    List<AdmissionDocumentCustody> findByAdmissionFormIdOrderByDocumentType(Long admissionId);
    Optional<AdmissionDocumentCustody> findByAdmissionFormIdAndDocumentType(Long admissionId,String documentType);
}
