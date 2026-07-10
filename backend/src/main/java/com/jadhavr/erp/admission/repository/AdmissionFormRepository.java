package com.jadhavr.erp.admission.repository;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdmissionFormRepository extends JpaRepository<AdmissionForm, Long>,
        JpaSpecificationExecutor<AdmissionForm> {
    Optional<AdmissionForm> findByAdmissionReferenceNumber(String admissionReferenceNumber);
    boolean existsByAdmissionReferenceNumber(String admissionReferenceNumber);
    boolean existsByEmailAndCollegeIdAndStatusNotIn(
            String email, Long collegeId, Collection<AdmissionStatus> statuses);
    Optional<AdmissionForm> findTopByStudentUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AdmissionForm> findTopByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<AdmissionForm> findByCollegeId(Long collegeId);
    List<AdmissionForm> findByDepartmentId(Long departmentId);
}
