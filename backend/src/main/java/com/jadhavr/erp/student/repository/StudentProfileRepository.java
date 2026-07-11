package com.jadhavr.erp.student.repository;

import com.jadhavr.erp.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long>,
        JpaSpecificationExecutor<StudentProfile> {
    Optional<StudentProfile> findByUserId(Long userId);
    Optional<StudentProfile> findByAdmissionNumber(String admissionNumber);
    boolean existsByAdmissionNumber(String admissionNumber);
    boolean existsByRollNumber(String rollNumber);
    boolean existsByEmailAndCollegeId(String email, Long collegeId);
    List<StudentProfile> findByCollegeId(Long collegeId);
    List<StudentProfile> findByDepartmentId(Long departmentId);
}
