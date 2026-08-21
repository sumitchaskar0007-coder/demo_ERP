package com.collegeerp.erp.academic.repository;

import com.collegeerp.erp.academic.entity.SemesterOffering;
import com.collegeerp.erp.academic.enums.SemesterOfferingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterOfferingRepository extends JpaRepository<SemesterOffering, Long> {
    @EntityGraph(attributePaths={"academicYear","academicTerm","curriculumSemester","department"})
    List<SemesterOffering> findByAcademicYearIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(Long yearId);
    @EntityGraph(attributePaths={"academicYear","academicTerm","curriculumSemester","department"})
    List<SemesterOffering> findByAcademicTermIdOrderByDepartmentIdAscCurriculumSemesterSemesterNumberAsc(Long termId);
    Optional<SemesterOffering> findByAcademicTermIdAndCurriculumSemesterId(Long termId, Long semesterId);
    Optional<SemesterOffering> findByIdAndCollegeId(Long id, Long collegeId);
    List<SemesterOffering> findByAcademicTermIdAndStatus(Long termId, SemesterOfferingStatus status);
}
