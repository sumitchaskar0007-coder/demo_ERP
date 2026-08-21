package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.staff.entity.StaffProfile;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="class_teacher_assignment_history")
public class ClassTeacherAssignmentHistory extends BaseAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="section_id") private Section section;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="previous_teacher_id") private StaffProfile previousTeacher;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="new_teacher_id") private StaffProfile newTeacher;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="assigned_by_id") private StaffProfile assignedBy;
    @Column(nullable=false) private LocalDateTime assignedAt=LocalDateTime.now();
    public Long getId(){return id;} public Section getSection(){return section;} public void setSection(Section v){section=v;}
    public StaffProfile getPreviousTeacher(){return previousTeacher;} public void setPreviousTeacher(StaffProfile v){previousTeacher=v;}
    public StaffProfile getNewTeacher(){return newTeacher;} public void setNewTeacher(StaffProfile v){newTeacher=v;}
    public StaffProfile getAssignedBy(){return assignedBy;} public void setAssignedBy(StaffProfile v){assignedBy=v;}
    public LocalDateTime getAssignedAt(){return assignedAt;}
}
