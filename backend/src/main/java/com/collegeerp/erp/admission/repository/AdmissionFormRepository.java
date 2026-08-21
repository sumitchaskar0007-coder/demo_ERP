package com.collegeerp.erp.admission.repository;

import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.fee.dto.AdmissionStatusCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select admission from AdmissionForm admission where admission.id = :id")
    Optional<AdmissionForm> findByIdForUpdate(@Param("id") Long id);

    List<AdmissionForm> findByCollegeId(Long collegeId);
    List<AdmissionForm> findByDepartmentId(Long departmentId);
    long countByCollegeIdAndStatus(Long collegeId, AdmissionStatus status);
    long countByCollegeIdAndStatusIn(Long collegeId, Collection<AdmissionStatus> statuses);
    long countByStatusIn(Collection<AdmissionStatus> statuses);
    long countByCollegeIdAndDepartmentIdAndStatusIn(
            Long collegeId, Long departmentId, Collection<AdmissionStatus> statuses);
    long countByCollegeIdAndPrintCountGreaterThan(Long collegeId, Integer printCount);

    @Query("""
            select new com.collegeerp.erp.fee.dto.AdmissionStatusCount(a.status, count(a.id))
            from AdmissionForm a
            group by a.status
            """)
    List<AdmissionStatusCount> countAdmissionsByStatus();
}
