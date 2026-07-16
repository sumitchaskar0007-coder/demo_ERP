package com.jadhavr.erp.staff.entity;

import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.staff.enums.StaffStatus;
import com.jadhavr.erp.staff.enums.StaffType;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "staff_profiles")
public class StaffProfile extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "staff_profile_departments",
            joinColumns = @JoinColumn(name = "staff_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_staff_profile_department",
                    columnNames = {"staff_profile_id", "department_id"}))
    private Set<Department> departments = new LinkedHashSet<>();

    @Column(nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StaffType staffType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffStatus status = StaffStatus.ACTIVE;

    private LocalDate joiningDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Set<Department> getDepartments() {
        if (departments.isEmpty() && department != null) departments.add(department);
        return departments;
    }
    public void setDepartments(Set<Department> departments) {
        this.departments = departments == null ? new LinkedHashSet<>() : new LinkedHashSet<>(departments);
        if (department != null) this.departments.add(department);
    }
    public boolean belongsToDepartment(Long departmentId) {
        return departmentId != null && ((department != null && departmentId.equals(department.getId()))
                || departments.stream().anyMatch(item -> departmentId.equals(item.getId())));
    }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public StaffType getStaffType() { return staffType; }
    public void setStaffType(StaffType staffType) { this.staffType = staffType; }
    public StaffStatus getStatus() { return status; }
    public void setStatus(StaffStatus status) { this.status = status; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
}
