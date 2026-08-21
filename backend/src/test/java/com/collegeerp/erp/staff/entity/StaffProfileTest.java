package com.collegeerp.erp.staff.entity;

import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.user.entity.Role;
import com.collegeerp.erp.user.entity.RoleName;
import com.collegeerp.erp.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffProfileTest {

    @Test
    void principalTeachingProfileCanTeachAcrossCollegeDepartments() {
        Role principal = new Role();
        principal.setName(RoleName.PRINCIPAL);
        User user = new User();
        user.setRoles(Set.of(principal));
        StaffProfile profile = new StaffProfile();
        profile.setUser(user);

        assertTrue(profile.belongsToDepartment(42L));
    }

    @Test
    void regularTeacherRemainsLimitedToAssignedDepartments() {
        User user = new User();
        user.setRoles(Set.of());
        Department department = new Department();
        department.setId(7L);
        StaffProfile profile = new StaffProfile();
        profile.setUser(user);
        profile.setDepartment(department);

        assertTrue(profile.belongsToDepartment(7L));
        assertFalse(profile.belongsToDepartment(8L));
    }

    @Test
    void hodCanTeachCollegeWideWithoutExpandingAdministrativeDepartmentScope() {
        Role hod = new Role();
        hod.setName(RoleName.HOD);
        User user = new User();
        user.setRoles(Set.of(hod));
        Department assignedDepartment = new Department();
        assignedDepartment.setId(7L);
        StaffProfile profile = new StaffProfile();
        profile.setUser(user);
        profile.setDepartment(assignedDepartment);

        assertTrue(profile.canTeachInDepartment(8L));
        assertFalse(profile.belongsToDepartment(8L));
    }
}
