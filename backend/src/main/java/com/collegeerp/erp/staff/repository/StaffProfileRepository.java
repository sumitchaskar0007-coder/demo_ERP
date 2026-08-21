package com.collegeerp.erp.staff.repository;

import com.collegeerp.erp.staff.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.staff.enums.StaffStatus;
import com.collegeerp.erp.staff.enums.StaffType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long>,
        JpaSpecificationExecutor<StaffProfile> {
    Optional<StaffProfile> findByUserId(Long userId);
    Optional<StaffProfile> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    List<StaffProfile> findByCollegeId(Long collegeId);
    List<StaffProfile> findByUserIdIn(Collection<Long> userIds);
    @EntityGraph(attributePaths = {"user", "college", "department", "departments"})
    @Query("""
            select distinct staff
            from StaffProfile staff
            left join staff.departments assignedDepartment
            where staff.department.id = :departmentId
               or assignedDepartment.id = :departmentId
            """)
    List<StaffProfile> findByAssignedDepartmentId(@Param("departmentId") Long departmentId);
    @EntityGraph(attributePaths = {"user", "college", "department", "departments"})
    @Query("""
            select distinct staff
            from StaffProfile staff
            left join staff.departments assignedDepartment
            left join staff.user.roles staffRole
            where staff.college.id = :collegeId
              and staff.status = :status
              and staff.staffType in :staffTypes
              and (staff.department.id = :departmentId
                   or assignedDepartment.id = :departmentId
                   or staffRole.name in (com.collegeerp.erp.user.entity.RoleName.PRINCIPAL,
                                         com.collegeerp.erp.user.entity.RoleName.HOD))
            order by staff.fullName
            """)
    List<StaffProfile> findTeachingByCollegeAndDepartment(
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("status") StaffStatus status,
            @Param("staffTypes") Collection<StaffType> staffTypes);
    long countByCollegeId(Long collegeId);
    @Query("select count(distinct s.id) from StaffProfile s join s.user u join u.roles r "
            + "where s.college.id = :collegeId and r.name in :roleNames")
    long countTeachingStaffByCollegeId(
            @Param("collegeId") Long collegeId,
            @Param("roleNames") Collection<RoleName> roleNames);
    boolean existsByDepartmentIdAndStaffTypeAndStatus(Long departmentId, com.collegeerp.erp.staff.enums.StaffType type, com.collegeerp.erp.staff.enums.StaffStatus status);
    boolean existsByDepartmentIdAndStaffTypeAndStatusAndIdNot(
            Long departmentId,
            com.collegeerp.erp.staff.enums.StaffType type,
            com.collegeerp.erp.staff.enums.StaffStatus status,
            Long id);
}
