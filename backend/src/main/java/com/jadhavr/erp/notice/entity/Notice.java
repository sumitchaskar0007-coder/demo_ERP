package com.jadhavr.erp.notice.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notices")
public class Notice extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "notice_colleges", joinColumns = @JoinColumn(name = "notice_id"),
            inverseJoinColumns = @JoinColumn(name = "college_id"))
    private Set<com.jadhavr.erp.college.entity.College> colleges = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")
    private Department department;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notice_audience_roles", joinColumns = @JoinColumn(name = "notice_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 40)
    private Set<RoleName> audienceRoles = new HashSet<>();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Set<com.jadhavr.erp.college.entity.College> getColleges() { return colleges; }
    public void setColleges(Set<com.jadhavr.erp.college.entity.College> colleges) { this.colleges = colleges; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Set<RoleName> getAudienceRoles() { return audienceRoles; }
    public void setAudienceRoles(Set<RoleName> audienceRoles) { this.audienceRoles = audienceRoles; }
}
