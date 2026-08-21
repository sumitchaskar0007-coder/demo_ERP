package com.collegeerp.erp.admission.repository;

import com.collegeerp.erp.admission.entity.AdmissionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface AdmissionStatusHistoryRepository extends JpaRepository<AdmissionStatusHistory, Long> {
    List<AdmissionStatusHistory> findByAdmissionFormIdOrderByCreatedAtAsc(Long admissionFormId);
    List<AdmissionStatusHistory> findByAdmissionFormIdInOrderByCreatedAtAsc(Collection<Long> admissionFormIds);
}
