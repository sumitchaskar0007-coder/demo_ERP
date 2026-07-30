package com.jadhavr.erp.admission.repository;

import com.jadhavr.erp.admission.entity.AdmissionDocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AdmissionDocumentRequirementRepository
        extends JpaRepository<AdmissionDocumentRequirement, Long> {
    List<AdmissionDocumentRequirement> findByDepartmentIdOrderByDisplayOrderAscIdAsc(
            Long departmentId);
    List<AdmissionDocumentRequirement> findByDepartmentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long departmentId);
    Optional<AdmissionDocumentRequirement> findByIdAndDepartmentId(Long id, Long departmentId);
    Optional<AdmissionDocumentRequirement> findByDepartmentIdAndDocumentKeyAndActiveTrue(
            Long departmentId, String documentKey);
    boolean existsByDepartmentIdAndDocumentNameIgnoreCase(Long departmentId, String documentName);

    @Query("""
            select requirement.documentKey
            from AdmissionDocumentRequirement requirement
            where requirement.department.id = :departmentId
              and requirement.active = true
              and requirement.required = true
            """)
    Set<String> findRequiredKeys(@Param("departmentId") Long departmentId);
}
