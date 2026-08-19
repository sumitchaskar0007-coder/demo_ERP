package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.AcademicModels.AcademicTerm;
import com.jadhavr.erp.academic.enums.AcademicTermStatus;
import com.jadhavr.erp.academic.enums.AcademicTermType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    List<AcademicTerm> findByAcademicYearIdOrderByStartDate(Long academicYearId);
    Optional<AcademicTerm> findByCollegeIdAndStatus(Long collegeId, AcademicTermStatus status);
    Optional<AcademicTerm> findByIdAndCollegeId(Long id, Long collegeId);
    Optional<AcademicTerm> findByAcademicYearIdAndTermType(Long yearId, AcademicTermType type);
}
