package com.collegeerp.erp.user.repository;

import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);
}
