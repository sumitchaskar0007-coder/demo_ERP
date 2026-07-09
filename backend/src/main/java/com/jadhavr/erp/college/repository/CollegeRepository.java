package com.jadhavr.erp.college.repository;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long>,
        JpaSpecificationExecutor<College> {

    Optional<College> findByCode(String code);

    boolean existsByCode(String code);

    List<College> findByStatus(CollegeStatus status);
}
