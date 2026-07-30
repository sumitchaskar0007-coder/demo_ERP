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
    List<AdmissionDocumentRequirement> findByCollegeIdOrderByDisplayOrderAscIdAsc(Long collegeId);
    List<AdmissionDocumentRequirement> findByCollegeIdAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long collegeId);
    Optional<AdmissionDocumentRequirement> findByIdAndCollegeId(Long id, Long collegeId);
    Optional<AdmissionDocumentRequirement> findByCollegeIdAndDocumentKeyAndActiveTrue(
            Long collegeId, String documentKey);
    boolean existsByCollegeIdAndDocumentNameIgnoreCase(Long collegeId, String documentName);

    @Query("""
            select requirement.documentKey
            from AdmissionDocumentRequirement requirement
            where requirement.college.id = :collegeId
              and requirement.active = true
              and requirement.required = true
            """)
    Set<String> findRequiredKeys(@Param("collegeId") Long collegeId);
}
