package com.collegeerp.erp.academic.repository;

import com.collegeerp.erp.academic.entity.SemesterRolloverItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRolloverItemRepository extends JpaRepository<SemesterRolloverItem, Long> {
    List<SemesterRolloverItem> findByJobIdOrderByStudentId(Long jobId);
}
