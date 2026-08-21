package com.collegeerp.erp.academic.repository;

import com.collegeerp.erp.academic.entity.AcademicModels.AcademicYear;
import com.collegeerp.erp.academic.enums.AcademicYearStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    List<AcademicYear> findByCollegeIdOrderByStartDateDesc(Long collegeId);
    Optional<AcademicYear> findByCollegeIdAndStatus(Long collegeId, AcademicYearStatus status);
    Optional<AcademicYear> findByCollegeIdAndName(Long collegeId, String name);
    @Query("select y from ManagedAcademicYear y where y.college.id=:collegeId and replace(y.name,'/','-')=replace(:name,'/','-')")
    Optional<AcademicYear> findByCollegeIdAndNormalizedName(@Param("collegeId") Long collegeId,
            @Param("name") String name);
    Optional<AcademicYear> findByIdAndCollegeId(Long id, Long collegeId);
    boolean existsByCollegeIdAndNameIgnoreCase(Long collegeId, String name);
    List<AcademicYear> findByGlobalAcademicYearId(Long globalAcademicYearId);
    long countByGlobalAcademicYearId(Long globalAcademicYearId);
}
