package com.collegeerp.erp.academic.repository;

import com.collegeerp.erp.academic.entity.GlobalAcademicYear;
import com.collegeerp.erp.academic.enums.AcademicYearStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlobalAcademicYearRepository extends JpaRepository<GlobalAcademicYear, Long> {
    List<GlobalAcademicYear> findAllByOrderByStartDateDesc();
    Optional<GlobalAcademicYear> findByStatus(AcademicYearStatus status);
    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select count(y) from GlobalAcademicYear y
            where y.id <> :excludedId
              and y.startDate <= :endDate and y.endDate >= :startDate
            """)
    long countOverlapping(@Param("excludedId") Long excludedId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
