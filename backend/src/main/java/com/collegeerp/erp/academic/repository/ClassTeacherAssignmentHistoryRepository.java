package com.collegeerp.erp.academic.repository;
import com.collegeerp.erp.academic.entity.ClassTeacherAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClassTeacherAssignmentHistoryRepository extends JpaRepository<ClassTeacherAssignmentHistory,Long>{List<ClassTeacherAssignmentHistory> findTop20BySectionDepartmentIdOrderByAssignedAtDesc(Long departmentId);}
