package com.jadhavr.erp.student.repository;

import com.jadhavr.erp.student.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);
    Optional<StudentProfile> findByAdmissionNumber(String admissionNumber);
    boolean existsByAdmissionNumber(String admissionNumber);
    boolean existsByEmailAndCollegeId(String email, Long collegeId);
    List<StudentProfile> findByCollegeId(Long collegeId);
    List<StudentProfile> findByDepartmentId(Long departmentId);
}
