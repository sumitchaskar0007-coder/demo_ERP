package com.jadhavr.erp.academic.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.student.entity.StudentProfile;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="student_division_transfers")
public class StudentDivisionTransfer extends BaseAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id") private StudentProfile student;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="from_section_id") private Section fromSection;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="to_section_id") private Section toSection;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="changed_by_id") private StaffProfile changedBy;
    @Column(nullable=false) private LocalDateTime transferredAt=LocalDateTime.now();
    public Long getId(){return id;} public StudentProfile getStudent(){return student;} public void setStudent(StudentProfile v){student=v;}
    public Section getFromSection(){return fromSection;} public void setFromSection(Section v){fromSection=v;}
    public Section getToSection(){return toSection;} public void setToSection(Section v){toSection=v;}
    public StaffProfile getChangedBy(){return changedBy;} public void setChangedBy(StaffProfile v){changedBy=v;}
    public LocalDateTime getTransferredAt(){return transferredAt;}
}
