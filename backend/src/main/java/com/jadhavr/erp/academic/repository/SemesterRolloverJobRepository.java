package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.SemesterRolloverJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRolloverJobRepository extends JpaRepository<SemesterRolloverJob, Long> {
    Optional<SemesterRolloverJob> findByCollegeIdAndSourceTermIdAndTargetTermId(Long collegeId, Long sourceId, Long targetId);
    List<SemesterRolloverJob> findByCollegeIdOrderByCreatedAtDesc(Long collegeId);
}
