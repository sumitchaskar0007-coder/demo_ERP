package com.jadhavr.erp.staff.repository;

import com.jadhavr.erp.staff.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;

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
            where staff.college.id = :collegeId
              and staff.status = :status
              and staff.staffType in :staffTypes
              and (staff.department.id = :departmentId or assignedDepartment.id = :departmentId)
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
    boolean existsByDepartmentIdAndStaffTypeAndStatus(Long departmentId, com.jadhavr.erp.staff.enums.StaffType type, com.jadhavr.erp.staff.enums.StaffStatus status);
    boolean existsByDepartmentIdAndStaffTypeAndStatusAndIdNot(
            Long departmentId,
            com.jadhavr.erp.staff.enums.StaffType type,
            com.jadhavr.erp.staff.enums.StaffStatus status,
            Long id);
}
