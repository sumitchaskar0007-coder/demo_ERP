package com.collegeerp.erp.college.repository;

import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.entity.CollegeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long>,
        JpaSpecificationExecutor<College> {

    Optional<College> findByCode(String code);

    boolean existsByCode(String code);

    List<College> findByStatus(CollegeStatus status);

    long countByStatus(CollegeStatus status);
}
