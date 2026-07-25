package com.jadhavr.erp.notice.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.user.entity.RoleName;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'NORMAL'")
    private NoticePriority priority = NoticePriority.NORMAL;
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
    @Column(name = "action_path", length = 500)
    private String actionPath;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private User deletedBy;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NoticePriority getPriority() { return priority; }
    public void setPriority(NoticePriority value) { priority = value == null ? NoticePriority.NORMAL : value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Set<com.jadhavr.erp.college.entity.College> getColleges() { return colleges; }
    public void setColleges(Set<com.jadhavr.erp.college.entity.College> colleges) { this.colleges = colleges; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Set<RoleName> getAudienceRoles() { return audienceRoles; }
    public void setAudienceRoles(Set<RoleName> audienceRoles) { this.audienceRoles = audienceRoles; }
    public String getActionPath() { return actionPath; }
    public void setActionPath(String actionPath) { this.actionPath = actionPath; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public User getDeletedBy() { return deletedBy; }
    public void setDeletedBy(User deletedBy) { this.deletedBy = deletedBy; }
}
