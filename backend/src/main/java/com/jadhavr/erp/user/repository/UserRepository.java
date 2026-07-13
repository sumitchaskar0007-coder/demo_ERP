package com.jadhavr.erp.user.repository;

import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByCollegeId(Long collegeId);
    boolean existsByCollegeIdAndRolesNameAndStatus(
            Long collegeId, RoleName roleName, UserStatus status);

    boolean existsByCollegeIdAndRolesNameAndStatusAndIdNot(
            Long collegeId, RoleName roleName, UserStatus status, Long id);

    @Query("select count(distinct u.id) from User u join u.roles r where r.name = :role")
    long countByRole(@Param("role") RoleName role);
}
