package com.jadhavr.erp.user.repository;

import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = {"college", "roles"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findAuthorizationById(@Param("id") Long id);
    boolean existsByEmail(String email);
    List<User> findByCollegeId(Long collegeId);
    boolean existsByCollegeIdAndRolesNameAndStatus(
            Long collegeId, RoleName roleName, UserStatus status);

    boolean existsByCollegeIdAndRolesNameAndStatusAndIdNot(
            Long collegeId, RoleName roleName, UserStatus status, Long id);

    @Query("select count(distinct u.id) from User u join u.roles r where r.name = :role")
    long countByRole(@Param("role") RoleName role);

    @EntityGraph(attributePaths = {"college", "roles"})
    @Query(value = """
            select distinct u from User u
            join u.roles role
            left join StudentProfile student on student.user = u
            left join StaffProfile staff on staff.user = u
            left join staff.departments assignedDepartment
            where u.status = com.jadhavr.erp.user.entity.UserStatus.ACTIVE
              and u.id <> :senderId
              and not exists (select blockedRole.id from u.roles blockedRole
                  where blockedRole.name in (com.jadhavr.erp.user.entity.RoleName.SUPER_ADMIN,
                                             com.jadhavr.erp.user.entity.RoleName.ADMIN))
              and (:collegeId is null or u.college.id = :collegeId)
              and (:departmentId is null
                   or student.department.id = :departmentId
                   or staff.department.id = :departmentId
                   or assignedDepartment.id = :departmentId)
              and (:roleName is null or role.name = :roleName)
              and (:keyword = ''
                   or lower(u.fullName) like concat('%', lower(:keyword), '%')
                   or lower(u.email) like concat('%', lower(:keyword), '%')
                   or u.phone like concat('%', :keyword, '%'))
            """,
            countQuery = """
            select count(distinct u.id) from User u
            join u.roles role
            left join StudentProfile student on student.user = u
            left join StaffProfile staff on staff.user = u
            left join staff.departments assignedDepartment
            where u.status = com.jadhavr.erp.user.entity.UserStatus.ACTIVE
              and u.id <> :senderId
              and not exists (select blockedRole.id from u.roles blockedRole
                  where blockedRole.name in (com.jadhavr.erp.user.entity.RoleName.SUPER_ADMIN,
                                             com.jadhavr.erp.user.entity.RoleName.ADMIN))
              and (:collegeId is null or u.college.id = :collegeId)
              and (:departmentId is null
                   or student.department.id = :departmentId
                   or staff.department.id = :departmentId
                   or assignedDepartment.id = :departmentId)
              and (:roleName is null or role.name = :roleName)
              and (:keyword = ''
                   or lower(u.fullName) like concat('%', lower(:keyword), '%')
                   or lower(u.email) like concat('%', lower(:keyword), '%')
                   or u.phone like concat('%', :keyword, '%'))
            """)
    Page<User> searchNoticeRecipients(
            @Param("senderId") Long senderId,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("roleName") RoleName roleName,
            @Param("keyword") String keyword,
            Pageable pageable);
}
