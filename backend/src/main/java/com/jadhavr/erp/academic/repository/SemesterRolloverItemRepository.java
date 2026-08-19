package com.jadhavr.erp.academic.repository;

import com.jadhavr.erp.academic.entity.SemesterRolloverItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRolloverItemRepository extends JpaRepository<SemesterRolloverItem, Long> {
    List<SemesterRolloverItem> findByJobIdOrderByStudentId(Long jobId);
}
