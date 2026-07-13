package com.jadhavr.erp.staff.repository;

import com.jadhavr.erp.staff.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long>,
        JpaSpecificationExecutor<StaffProfile> {
    Optional<StaffProfile> findByUserId(Long userId);
    Optional<StaffProfile> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    List<StaffProfile> findByCollegeId(Long collegeId);
    boolean existsByDepartmentIdAndStaffTypeAndStatus(Long departmentId, com.jadhavr.erp.staff.enums.StaffType type, com.jadhavr.erp.staff.enums.StaffStatus status);
}
