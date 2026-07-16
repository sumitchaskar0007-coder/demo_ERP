package com.jadhavr.erp.department.repository;

import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.department.entity.DepartmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>,
        JpaSpecificationExecutor<Department> {

    boolean existsByCollegeIdAndCode(Long collegeId, String code);

    Optional<Department> findByCollegeIdAndCode(Long collegeId, String code);

    List<Department> findByCollegeId(Long collegeId);

    long countByCollegeId(Long collegeId);

    List<Department> findByCollegeIdAndStatus(Long collegeId, DepartmentStatus status);
}
